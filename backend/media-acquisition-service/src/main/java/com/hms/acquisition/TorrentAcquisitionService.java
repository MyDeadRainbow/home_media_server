package com.hms.acquisition;

import org.springframework.stereotype.Service;

@Service
public class TorrentAcquisitionService {

    private final VirusScannerService virusScannerService;

    public TorrentAcquisitionService(VirusScannerService virusScannerService) {
        this.virusScannerService = virusScannerService;
    }

    public ImportMediaResponse importMedia(ImportMediaRequest request) {
        String quality = request.quality() == null || request.quality().isBlank() ? "1080p" : request.quality();

        // This simulates an integration pipeline: tracker search -> download -> scan -> organize.
        String torrentSource = "https://tracker.example/search?q=" + request.title().replace(" ", "+");
        String downloadFolder = "downloads/torrents/" + request.title().replace(" ", "_").toLowerCase();
        boolean safe = virusScannerService.scanFolder(downloadFolder);
        String organizedPath = "media-library/" + request.type().toLowerCase() + "/" + request.title().replace(" ", "_") + "_" + quality;

        String status = safe ? "QUEUED_FOR_LIBRARY_INDEX" : "BLOCKED_BY_SECURITY_SCAN";
        return new ImportMediaResponse(request.title(), torrentSource, downloadFolder, safe, organizedPath, status);
    }
}
