package com.hms.acquisition;

public record ImportMediaResponse(
        String title,
        String torrentSource,
        String downloadPath,
        boolean virusScanPassed,
        String organizedPath,
        String status
) {
}
