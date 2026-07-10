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

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
