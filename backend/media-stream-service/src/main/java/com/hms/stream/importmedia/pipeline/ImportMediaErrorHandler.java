package com.hms.stream.importmedia.pipeline;

import com.hms.stream.importmedia.ImportMediaEntry;

@FunctionalInterface
public interface ImportMediaErrorHandler {
    void handleError(ImportMediaEntry entry, Throwable e);
}
