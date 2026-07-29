package com.hms.catalog.media;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.hms.catalog.MediaItem;
import com.hms.shared.media.Episode;
import com.hms.shared.media.MediaCategory;
import com.hms.shared.media.MediaInfo;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;

@Service
public class MediaCatalogService {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(MediaCatalogService.class);

    public MediaCatalogService() {
    }

    public List<MediaInfo> search(String query, MediaCategory category) {

        List<MediaInfo> mediaInfos = new ArrayList<>();

        try {
            mediaInfos = new MediaInfo.Dao().search(normalizeQuery(query));
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query media items from database: {}", e.getMessage(), e);
        }

        return mediaInfos;
    }

    public MediaItem add(CreateMediaRequest request) {
        MediaItem item = new MediaItem(
                UUID.randomUUID().toString(),
                request.title(),
                request.type(),
                request.year(),
                request.description(),
                request.posterUrl(),
                request.streamUrl());
        try {
            new MediaItem.Dao().insert(item);
        } catch (Exception e) {
            // throw new RuntimeException("Failed to insert media item into database", e);
        }
        
        return item;
    }

    public List<Series> getSeries(String query) {
        List<Series> seriesList = new ArrayList<>();
        try {
            seriesList = new Series.Dao().select(Map.of());
            String normalized = normalizeQuery(query);
            if (!normalized.isEmpty()) {
                seriesList = seriesList.stream()
                        .filter(series -> series.title() != null
                                && series.title().toLowerCase(Locale.ROOT).contains(normalized))
                        .toList();
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query series from database: {}", e.getMessage(), e);
        }
        return seriesList;
    }

    public Series getSeriesById(String seriesId) {
        try {
            return new Series.Dao().get(seriesId);
        } catch (SQLException e) {
            LOG.error("Failed to query series by ID from database: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<Season> getSeason(String seriesId, String query) {
        List<Season> seasonList = new ArrayList<>();
        try {
            seasonList = new Season.Dao().select(Map.of("seriesId", seriesId));
            String normalized = normalizeQuery(query);
            if (!normalized.isEmpty()) {
                seasonList = seasonList.stream()
                        .filter(season -> season.title() != null
                                && season.title().toLowerCase(Locale.ROOT).contains(normalized))
                        .toList();
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query seasons from database: {}", e.getMessage(), e);
        }
        return seasonList;
    }

    public List<Episode> getEpisodes(String seriesId, String seasonId, String query) {
        List<Episode> episodeList = new ArrayList<>();
        try {
            var episodes = new Episode.Dao()
                    .select(Map.of("seriesId", seriesId, "seasonId", seasonId));
            String normalized = normalizeQuery(query);
            for (var episode : episodes) {
                if (!normalized.isEmpty() && (episode.title() == null
                        || !episode.title().toLowerCase(Locale.ROOT).contains(normalized))) {
                    continue;
                }

                episodeList.add(episode);
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query episodes from database: {}", e.getMessage(), e);
        }
        return episodeList;
    }

    public List<Movie> getMovies(String query) {
        List<Movie> movieList = new ArrayList<>();
        try {
            List<Movie> movies = new Movie.Dao().select(Map.of());
            String normalized = normalizeQuery(query);
            for (Movie movie : movies) {
                if (!normalized.isEmpty() && (movie.title() == null
                        || !movie.title().toLowerCase(Locale.ROOT).contains(normalized))) {
                    continue;
                }

                movieList.add(movie);
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query movies from database: {}", e.getMessage(), e);
        }
        return movieList;
    }

    public Movie getMovieById(String id) {
        try {
            return new Movie.Dao().get(id);
        } catch (SQLException e) {
            LOG.error("Failed to query movie by ID from database: {}", e.getMessage(), e);
            return null;
        }
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
