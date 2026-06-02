package com.hms.catalog;

import java.util.List;

public record MediaItem(
        String id,
        String title,
        String type,
        Integer year,
        String description,
        String posterUrl,
        String streamUrl,
        List<String> subtitleLanguages
) {
}
