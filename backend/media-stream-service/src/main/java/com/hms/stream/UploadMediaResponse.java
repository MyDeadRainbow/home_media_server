package com.hms.stream;

public record UploadMediaResponse(
        String storageId,
        String playbackUrl,
        String originalFilename,
        String contentType,
        long size
) {
}
