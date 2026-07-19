package com.hms.stream.importmedia.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hms.stream.importmedia.ImportMediaEntry;
import com.hms.shared.pipline.Handler;

public class ImportMediaPipeline implements Handler<ImportMediaEntry> {

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

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {
        for (ImportMediaHandler handler : handlers) {
            try {
                entry = handler.handle(entry);
            } catch (Throwable e) {
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
