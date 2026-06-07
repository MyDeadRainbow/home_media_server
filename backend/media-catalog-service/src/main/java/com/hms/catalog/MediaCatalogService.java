package com.hms.catalog;

import org.springframework.stereotype.Service;

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
                "Demo entry to validate UI and stream controls."
                // ,

                // "https://picsum.photos/seed/hms1/320/180",
                // "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                // List.of("en", "es")
                );
        mediaIndex.put(sample.id(), sample);
    }

    public List<MediaItem> search(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(mediaIndex.values());
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return mediaIndex.values().stream()
                .filter(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.type().toLowerCase(Locale.ROOT).contains(normalized)
                        || Optional.ofNullable(item.description()).orElse("").toLowerCase(Locale.ROOT)
                                .contains(normalized))
                .toList();
    }

    public MediaItem add(CreateMediaRequest request) {
        MediaItem item = new MediaItem(
                UUID.randomUUID().toString(),
                request.title(),
                request.type(),
                request.year(),
                request.description()
                // ,
                // request.posterUrl(),
                // request.streamUrl(),
                // List.of("en")
            );
        try {
            item.insert();
        } catch (Exception e) {
            // throw new RuntimeException("Failed to insert media item into database", e);
        }
        
        mediaIndex.put(item.id(), item);
        return item;
    }

    public Optional<MediaItem> findById(String id) {
        return Optional.ofNullable(mediaIndex.get(id));
    }
}
