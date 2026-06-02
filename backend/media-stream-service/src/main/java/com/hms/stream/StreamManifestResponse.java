package com.hms.stream;

import java.util.List;

public record StreamManifestResponse(
        String mediaId,
        String playbackUrl,
        List<CaptionTrack> captions
) {
}
