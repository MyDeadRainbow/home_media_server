package com.hms.stream.importmedia.pipeline;

import java.util.ArrayList;
import java.util.List;

public class ImportMediaPipelineBuilder {

    private List<ImportMediaHandler> handlers = new ArrayList<>();
    private ImportMediaErrorHandler errorHandler;

    protected ImportMediaPipelineBuilder() {
    }

    public ImportMediaPipelineBuilder addHandler(ImportMediaHandler handler) {
        handlers.add(handler);
        return this;
    }
    
    public ImportMediaPipelineBuilder onError(ImportMediaErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public ImportMediaPipeline build() {
        return new ImportMediaPipeline(handlers.toArray(new ImportMediaHandler[0]), errorHandler);
    }

}
