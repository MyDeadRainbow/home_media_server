package com.hms.catalog.media;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.hms.dao.SQLiteMap;
import com.hms.shared.media.Episode;
import com.hms.shared.media.MediaItem;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
import com.hms.shared.media.poster.Poster;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;

public class SeriesParser {

    private final Pattern seriesPattern = Pattern
            .compile("^(?<seriesName>.+)\\.S(?<seasonNumber>\\d+)E(?<episodeNumber>\\d+)\\.(?<episodeName>.+)\\..+$");

    private List<FilePathRecord> filePaths;

    protected SeriesParser(List<FilePathRecord> filePaths) {
        this.filePaths = filePaths;
    }

    public List<Series> parse() {
        // This must look at the file paths and determine what series, season and
        // episode each path belongs to
        // The.Office.US.S01E01.Pilot.720p.WEBRip.2CH.x265.HEVC-PSA

        SQLiteMap<Series> seriesMap = new SQLiteMap<>(new Series.Dao());

        for (FilePathRecord entry : filePaths) {
            String string = entry.filePath();
            seriesPattern.matcher(string).results().forEach(result -> {
                String seriesName = result.group("seriesName").replaceAll("\\.", " ");
                int seasonNumber = Integer.parseInt(result.group("seasonNumber"));
                int episodeNumber = Integer.parseInt(result.group("episodeNumber"));
                String episodeName = result.group("episodeName").replaceAll("\\.?[0-9]{3,4}p.*$", "").replaceAll("\\.",
                        " ");

                if (episodeName.isEmpty()) {
                    episodeName = "Episode " + episodeNumber;
                }

                Series series = seriesMap.values().stream()
                        .filter(s -> s.title().equals(seriesName))
                        .findFirst()
                        .orElse(null);

                if (series == null) {
                    series = Series.create(MetaData.create(seriesName, null, null, null, MetaDataStatus.PENDING, null),
                            Poster.create(null),
                            new ArrayList<Season>());
                    seriesMap.put(series.seriesId(), series);
                }

                Season season = series.seasons().stream()
                        .filter(s -> s.seasonNumber() == seasonNumber)
                        .findFirst()
                        .orElse(null);

                if (season == null) {
                    season = Season.create(series.seriesId(), seasonNumber,
                            MetaData.create(seriesName + " S" + seasonNumber, null, null, null, MetaDataStatus.PENDING,
                                    null),
                            Poster.create(null),
                            new ArrayList<Episode>());

                    series = series.addSeason(season);
                    seriesMap.put(series.seriesId(), series);
                }

                MediaItem mediaItem = new MediaItem(entry.mediaId(), string);

                Episode episode = Episode.create(season.seasonId(), series.seriesId(), episodeNumber, mediaItem,
                        MetaData.create(episodeName, null, null, null, MetaDataStatus.PENDING, null),
                        Poster.create(null));
                        
                season = season.addEpisode(episode);
                series = series.addSeason(season);
                seriesMap.put(series.seriesId(), series);
            });
        }

        return seriesMap.values().stream().toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<FilePathRecord> filePaths = new ArrayList<>();

        private Builder() {
        }

        public Builder addFilePath(FilePathRecord filePath) {
            filePaths.add(filePath);
            return this;
        }

        public Builder addFilePaths(List<FilePathRecord> filePaths) {
            this.filePaths.addAll(filePaths);
            return this;
        }

        public SeriesParser build() {
            return new SeriesParser(filePaths);
        }
    }
}
