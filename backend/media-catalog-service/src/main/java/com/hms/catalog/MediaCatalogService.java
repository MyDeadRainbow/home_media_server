package com.hms.catalog;

import org.springframework.stereotype.Service;

import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;
import com.hms.shared.dao.SQLiteSerializable;
// import com.hms.catalog.messaging.MediaUpdates;
import com.hms.shared.messaging.mediaupdates.MediaUpdate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MediaCatalogService {

    private final Map<String, MediaItem> mediaIndex = new ConcurrentHashMap<>();

    public MediaCatalogService() {
        MediaItem sample = new MediaItem(
                UUID.randomUUID().toString(),
                "Open Source Adventures",
                "series",
                2025,
                "Demo entry to validate UI and stream controls.",
                "https://picsum.photos/seed/hms1/320/180",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        mediaIndex.put(sample.id(), sample);
    }

    public List<MediaItem> search(String query) {
        List<MediaItem> results;
        try {
            results = SQLiteSerializable.select(MediaItem.class, Map.of());
        } catch (DBFileNotFoundException | GetConnectionException | SQLException e) {
            // Log the error and fall back to in-memory index
            System.err.println("Failed to query media items from database: " + e.getMessage());
            results = new ArrayList<>(mediaIndex.values());
        }
        if (query == null || query.isBlank()) {
            return results;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return results.stream()
                .filter(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.type().toLowerCase(Locale.ROOT).contains(normalized)
                        || Optional.ofNullable(item.description()).orElse("").toLowerCase(Locale.ROOT)
                                .contains(normalized))
                // .map(item -> new MediaItem(item.id(), item.title(), item.type(), item.year(), item.description(),
                //         item.posterUrl(), item.streamUrl()))
                .toList();
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
            item.insert();
        } catch (Exception e) {
            // throw new RuntimeException("Failed to insert media item into database", e);
        }

        // MediaUpdates.postMessage(MediaUpdate.created(item.id()));

        mediaIndex.put(item.id(), item);
        return item;
    }

    public Optional<MediaItem> findById(String id) {
        return Optional.ofNullable(mediaIndex.get(id));
    }
}
