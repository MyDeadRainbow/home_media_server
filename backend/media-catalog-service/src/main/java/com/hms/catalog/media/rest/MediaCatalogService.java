package com.hms.catalog.media.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.hms.catalog.MediaItem;
import com.hms.catalog.MediaItem.Dao;
import com.hms.catalog.media.Episode;
import com.hms.catalog.media.MediaInfo;
import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;
import com.hms.shared.media.MediaCategory;

@Service
public class MediaCatalogService {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(MediaCatalogService.class);
    // private final Map<String, MediaItem> mediaIndex = new ConcurrentHashMap<>();

    public MediaCatalogService() {
        // MediaItem sample = new MediaItem(
        //         UUID.randomUUID().toString(),
        //         "Open Source Adventures",
        //         "series",
        //         2025,
        //         "Demo entry to validate UI and stream controls.",
        //         "https://picsum.photos/seed/hms1/320/180",
        //         "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        // mediaIndex.put(sample.id(), sample);
    }

    public List<MediaInfo> search(String query, MediaCategory category) {

        List<MediaInfo> mediaInfos = new ArrayList<>();

        try {
            mediaInfos = new MediaInfo.Dao().search(query);
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

    // public Optional<MediaItem> findById(String id) {
    //     return Optional.ofNullable(mediaIndex.get(id));
    // }
}
