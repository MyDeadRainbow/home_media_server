package com.hms.stream;

public record CaptionTrack(
        String language,
        String label,
        String captionsUrl
) {
}
