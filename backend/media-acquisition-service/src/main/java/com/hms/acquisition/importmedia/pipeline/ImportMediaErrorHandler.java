package com.hms.acquisition.importmedia.pipeline;

import com.hms.acquisition.importmedia.ImportMediaEntry;

@FunctionalInterface
public interface ImportMediaErrorHandler {
    void handleError(ImportMediaEntry entry, Exception e);
}
