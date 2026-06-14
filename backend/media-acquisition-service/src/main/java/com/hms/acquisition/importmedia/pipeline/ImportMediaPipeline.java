package com.hms.acquisition.importmedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportMediaPipeline {

    private final Logger LOG = LoggerFactory.getLogger(ImportMediaPipeline.class);
    private final ImportMediaHandler[] handlers;
    private final ImportMediaErrorHandler errorHandler;

    protected ImportMediaPipeline(ImportMediaHandler[] handlers, ImportMediaErrorHandler errorHandler) {
        this.handlers = handlers;
        this.errorHandler = errorHandler;
    }

    public static ImportMediaPipelineBuilder builder() {
        return new ImportMediaPipelineBuilder();
    }

    public ImportMediaEntry executeHandlers(ImportMediaEntry entry) {
        for (ImportMediaHandler handler : handlers) {
            try {
                entry = handler.handle(entry);
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.handleError(entry, e);
                } else {
                    LOG.error("Error processing media import for entry: " + entry.id(), e);
                }
            }
        }
        return entry;
    }
    

}
