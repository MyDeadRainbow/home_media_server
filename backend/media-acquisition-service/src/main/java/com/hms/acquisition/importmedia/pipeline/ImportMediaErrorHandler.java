package com.hms.acquisition.importmedia;

@FunctionalInterface
public interface ImportMediaErrorHandler {
    void handleError(ImportMediaEntry entry, Exception e);
}
