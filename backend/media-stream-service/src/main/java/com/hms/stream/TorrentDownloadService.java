package com.hms.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hms.stream.importmedia.ImportMediaEntry;
import com.hms.stream.importmedia.ImportMediaRequest;
import com.hms.stream.importmedia.ImportMediaStatus;

@Service
public class TorrentDownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(TorrentDownloadService.class);

    public TorrentDownloadService() {
    }

    public boolean addImportRequest(ImportMediaRequest request) {
        ImportMediaEntry entry = new ImportMediaEntry(
                java.util.UUID.randomUUID().toString(),
                request.category(),
                request.title(),
                ImportMediaStatus.PENDING,
                request.magnetLink(),
                new java.sql.Date(System.currentTimeMillis()),
                null
            );

        try {
            entry.insert();
        } catch (Exception e) {
            LOG.error("Failed to add media request", e);
            return false;
        }

        return true;
    }
}
