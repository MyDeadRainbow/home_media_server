package com.hms.catalog.media.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.hms.catalog.MediaItem;
import com.hms.catalog.media.MediaInfo;
import com.hms.catalog.media.Movie;
import com.hms.catalog.media.Season;
import com.hms.catalog.media.Series;
import com.hms.shared.media.MediaCategory;

@Service
public class MediaCatalogService {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(MediaCatalogService.class);
    // private final Map<String, MediaItem> mediaIndex = new ConcurrentHashMap<>();

    public MediaCatalogService() {
        // MediaItem sample = new MediaItem(
        // UUID.randomUUID().toString(),
        // "Open Source Adventures",
        // "series",
        // 2025,
        // "Demo entry to validate UI and stream controls.",
        // "https://picsum.photos/seed/hms1/320/180",
        // "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        // mediaIndex.put(sample.id(), sample);
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

        // List<MediaItem> results;
        // try {
        // results = new MediaItem.Dao().select(Map.of());
        // } catch (SQLException e) {
        // // Log the error and fall back to in-memory index
        // System.err.println("Failed to query media items from database: " +
        // e.getMessage());
        // results = new ArrayList<>(mediaIndex.values());
        // }
        // if (query == null || query.isBlank()) {
        // return results;
        // }
        // String normalized = query.toLowerCase(Locale.ROOT);
        // return results.stream()
        // .filter(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized)
        // || item.type().toLowerCase(Locale.ROOT).contains(normalized)
        // ||
        // Optional.ofNullable(item.description()).orElse("").toLowerCase(Locale.ROOT)
        // .contains(normalized))
        // // .map(item -> new MediaItem(item.id(), item.title(), item.type(),
        // item.year(),
        // // item.description(),
        // // item.posterUrl(), item.streamUrl()))
        // .toList();
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

        // MediaUpdates.postMessage(MediaUpdate.created(item.id()));

        // mediaIndex.put(item.id(), item);
        return item;
    }

    public List<Series> getSeries(String query) {
        List<Series> seriesList = new ArrayList<>();
        try {
            seriesList = new Series.Dao().select(Map.of());
            String normalized = normalizeQuery(query);
            if (!normalized.isEmpty()) {
                seriesList = seriesList.stream()
                        .filter(series -> series.name() != null
                                && series.name().toLowerCase(Locale.ROOT).contains(normalized))
                        .toList();
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query series from database: {}", e.getMessage(), e);
        }
        return seriesList;
    }

    public List<Season> getSeason(String seriesId, String query) {
        List<Season> seasonList = new ArrayList<>();
        try {
            seasonList = new Season.Dao().select(Map.of("seriesId", seriesId));
            String normalized = normalizeQuery(query);
            if (!normalized.isEmpty()) {
                seasonList = seasonList.stream()
                        .filter(season -> season.name() != null
                                && season.name().toLowerCase(Locale.ROOT).contains(normalized))
                        .toList();
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query seasons from database: {}", e.getMessage(), e);
        }
        return seasonList;
    }

    public List<MediaInfo> getEpisodes(String seriesId, String seasonId, String query) {
        List<MediaInfo> episodeList = new ArrayList<>();
        try {
            var episodes = new com.hms.catalog.media.Episode.Dao()
                    .select(Map.of("seriesId", seriesId, "seasonId", seasonId));
            String normalized = normalizeQuery(query);
            for (var episode : episodes) {
                if (!normalized.isEmpty() && (episode.name() == null
                        || !episode.name().toLowerCase(Locale.ROOT).contains(normalized))) {
                    continue;
                }

                episodeList.add(new MediaInfo(
                        episode.media().mediaId(),
                        episode.name(),
                        MediaCategory.SERIES.name().toLowerCase(Locale.ROOT),
                        episode.metaData().airDate(),
                        episode.metaData().plotSummary(),
                        episode.metaData().rating(),
                        null,
                        episode.media().filePath()));
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query episodes from database: {}", e.getMessage(), e);
        }
        return episodeList;
    }

    public List<MediaInfo> getMovies(String query) {
        List<MediaInfo> movieList = new ArrayList<>();
        try {
            List<Movie> movies = new Movie.Dao().select(Map.of());
            String normalized = normalizeQuery(query);
            for (Movie movie : movies) {
                if (!normalized.isEmpty() && (movie.name() == null
                        || !movie.name().toLowerCase(Locale.ROOT).contains(normalized))) {
                    continue;
                }

                movieList.add(new MediaInfo(
                        movie.mediaItem().mediaId(),
                        movie.name(),
                        MediaCategory.MOVIE.name().toLowerCase(Locale.ROOT),
                        movie.metaData().airDate(),
                        movie.metaData().plotSummary(),
                        movie.metaData().rating(),
                        null,
                        movie.mediaItem().filePath()));
            }
        } catch (SQLException e) {
            // Log the error and fall back to in-memory index
            LOG.error("Failed to query movies from database: {}", e.getMessage(), e);
        }
        return movieList;
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
