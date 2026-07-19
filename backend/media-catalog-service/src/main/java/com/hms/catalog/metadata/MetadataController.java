package com.hms.catalog.metadata;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.catalog.TaskExecutor;
import com.hms.shared.media.Episode;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final Logger LOG = LoggerFactory.getLogger(MetadataController.class);
    private final TaskExecutor taskExecutor;

    public MetadataController(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/update/{metaDataId}")
    public ResponseEntity<MetaData> postMethodName(@PathVariable String metaDataId,
            @RequestBody MetaData entity) {
        try {
            MetaData oldMetaData = new MetaData.Dao().get(metaDataId);
            if (oldMetaData == null) {
                return ResponseEntity.notFound().build();
            }
            MetaData updatedMetaData = oldMetaData
                    .withAirDate(entity.airDate())
                    .withPlotSummary(entity.plotSummary())
                    .withRating(entity.rating())
                    .withStatus(entity.status())
                    .withMessage(entity.message())
                    .withTitle(entity.title());
            new MetaData.Dao().update(updatedMetaData);

            taskExecutor.submit(metaDataId, () -> {
                // Custom task logic here
                try {
                    // attempt to merge series metadata if applicable
                    Series series = new Series.Dao().select(Map.of("metaDataId", metaDataId)).stream().findFirst()
                            .orElse(null);
                    if (series != null) {
                        List<Season> seasons = series.seasons();

                        List<Series> otherSeries = new MetaData.Dao().select(Map.of("title", series.title()))
                                .stream()
                                .filter(md -> {
                                    try {
                                        return md.metaDataId() != metaDataId && new Series.Dao()
                                                .select(Map.of("metaDataId", md.metaDataId())).stream().findFirst()
                                                .orElse(null) != null;
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                })
                                .map(md -> {
                                    try {
                                        return new Series.Dao().select(Map.of("metaDataId", md.metaDataId())).stream()
                                                .findFirst().orElse(null);
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                })
                                .filter(s -> s != null)
                                .toList();

                        for (Series s : otherSeries) {
                            List<Season> otherSeasons = s.seasons();
                            // List<Season> seasonsToMerge = seasons.stream()
                            // .filter(season -> otherSeasons.stream()
                            // .anyMatch(otherSeason -> otherSeason.seasonNumber() ==
                            // season.seasonNumber()))
                            // .toList();

                            // Merge logic for like seasons
                            for (Season season : seasons) {
                                // Merge logic for each season
                                Season otherSeason = otherSeasons.stream()
                                        .filter(os -> os.seasonNumber() == season.seasonNumber())
                                        .findFirst()
                                        .orElse(null);
                                if (otherSeason != null) {
                                    // Merge episodes from both seasons
                                    List<Episode> otherEpisodes = otherSeason.episodes();
                                    for (Episode otherEpisode : otherEpisodes) {
                                        otherEpisode = otherEpisode
                                                .withSeasonId(season.seasonId())
                                                .withSeriesId(series.seriesId());
                                        new Episode.Dao().update(otherEpisode);
                                    }
                                    new Season.Dao().delete(otherSeason);
                                }
                            }

                            // Merge logic for seasons that don't exist in the current series
                            List<Season> otherSeasonsToMerge = otherSeasons.stream()
                                    .filter(otherSeason -> seasons.stream()
                                            .noneMatch(season -> season.seasonNumber() == otherSeason.seasonNumber()))
                                    .toList();
                            for (Season otherSeason : otherSeasonsToMerge) {
                                List<Episode> otherEpisodes = otherSeason.episodes();
                                for (Episode otherEpisode : otherEpisodes) {
                                    otherEpisode = otherEpisode
                                            .withSeriesId(series.seriesId());
                                    new Episode.Dao().update(otherEpisode);
                                }
                                otherSeason = otherSeason
                                        .withSeriesId(series.seriesId());
                                new Season.Dao().update(otherSeason);

                            }

                            s = new Series.Dao().get(s.seriesId());
                            new Series.Dao().delete(s);
                        }
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            return ResponseEntity.ok(updatedMetaData);
        } catch (SQLException e) {
            LOG.error("Error while updating metadata", e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/requestSearch/{metaDataId}")
    public ResponseEntity<String> postMethodName(@PathVariable String metaDataId) {
        try {
            MetaData metaData = new MetaData.Dao().get(metaDataId);
            if (metaData == null) {
                return ResponseEntity.notFound().build();
            }
            new MetaData.Dao().update(metaData.withStatus(MetaDataStatus.PENDING));
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(metaDataId);
    }

}
