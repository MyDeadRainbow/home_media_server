package com.hms.catalog.datamine;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.hms.catalog.datamine.api.MediaDbApiFactory;
import com.hms.shared.media.Episode;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
import com.hms.shared.util.PollingService;

@Service
public class DatamineService extends PollingService {

    @Override
    public Duration pollingInterval() {
        return Duration.ofSeconds(1);
    }

    @Override
    public void poll() {
        try {
            List<MetaData> metadataList = new MetaData.Dao().select(Map.of("status", MetaDataStatus.PENDING.name()));
            for (MetaData metadata : metadataList) {
                Episode episode = new Episode.Dao().select(Map.of("metaDataId", metadata.metaDataId())).stream()
                        .findFirst().orElse(null);
                if (episode != null) {
                    Series series = new Series.Dao().get(episode.seriesId());
                    if (series != null) {
                        submit(series.seriesId(), () -> {
                            try {
                                new Series.Dao().update(MediaDbApiFactory.createTMDBApi().searchSeries(series));
                            } catch (SQLException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        });
                        continue;
                    }
                }

                Season season = new Season.Dao().select(Map.of("metaDataId", metadata.metaDataId())).stream()
                        .findFirst().orElse(null);
                if (season != null) {
                    Series series = new Series.Dao().get(season.seriesId());
                    if (series != null) {
                        submit(series.seriesId(),
                                () -> {
                                    try {
                                        new Series.Dao()
                                                .update(MediaDbApiFactory.createTMDBApi().searchSeries(series));
                                    } catch (SQLException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                });
                        continue;
                    }
                }

                Series series = new Series.Dao().select(Map.of("metaDataId", metadata.metaDataId())).stream()
                        .findFirst().orElse(null);
                if (series != null) {
                    submit(series.seriesId(),
                            () -> {
                                try {
                                    new Series.Dao()
                                            .update(MediaDbApiFactory.createTMDBApi().searchSeries(series));
                                } catch (SQLException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            });
                    continue;
                }

                Movie movie = new Movie.Dao().select(Map.of("metaDataId", metadata.metaDataId())).stream().findFirst()
                        .orElse(null);
                if (movie != null) {
                    submit(movie.movieId(),
                            () -> {
                                try {
                                    new Movie.Dao().update(MediaDbApiFactory.createTMDBApi().searchMovie(movie));
                                } catch (SQLException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            });
                    continue;
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
