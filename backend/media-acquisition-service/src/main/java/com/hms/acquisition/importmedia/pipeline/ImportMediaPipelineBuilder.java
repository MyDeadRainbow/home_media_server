package com.hms.acquisition.importmedia;

import java.util.ArrayList;
import java.util.List;

public class ImportMediaPipelineBuilder {

    private List<ImportMediaHandler> handlers = new ArrayList<>();

    protected ImportMediaPipelineBuilder() {
    }

    public ImportMediaPipelineBuilder addHandler(ImportMediaHandler handler) {
        handlers.add(handler);
        return this;
    }

    public ImportMediaPipeline build() {
        return new ImportMediaPipeline(handlers.toArray(new ImportMediaHandler[0]));
    }

    public ImportMediaPipeline onError(ImportMediaHandler errorHandler) {
        handlers.add(errorHandler);
        return new ImportMediaPipeline(handlers.toArray(new ImportMediaHandler[0]));
    }
}
