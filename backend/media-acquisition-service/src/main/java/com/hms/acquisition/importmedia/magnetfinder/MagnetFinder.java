package com.hms.acquisition.importmedia.magnetfinder;

import com.hms.acquisition.importmedia.ImportMediaEntry;
import com.hms.acquisition.importmedia.ImportMediaStatus;
import com.hms.acquisition.importmedia.pipeline.ImportMediaHandler;

public abstract class MagnetFinder implements ImportMediaHandler {

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) throws Exception {
        if (entry.status() == ImportMediaStatus.MAGNET_FOUND) {
            return entry; // Skip processing if magnet link is already found
        }
        try {
            String magnetLink = findBestMagnetLink(entry);
            if (magnetLink != null) {
                entry = new ImportMediaEntry(entry.id(), entry.title(), entry.status(), magnetLink);
            } else {
                entry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.MAGNET_NOT_FOUND, null);
            }
            entry.update();
        } catch (Exception e) {
            // Log the error or handle it accordingly
            e.printStackTrace();
        }
        return entry;
    }

    protected abstract String findBestMagnetLink(ImportMediaEntry entry) throws Exception;
    
}
