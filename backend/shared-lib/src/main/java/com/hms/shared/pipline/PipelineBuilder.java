package com.hms.shared.pipline;

import java.util.ArrayList;
import java.util.List;

public class PipelineBuilder<T> {

    private List<Handler<T>> handlers = new ArrayList<>();
    private ErrorHandler<T> errorHandler;

    protected PipelineBuilder() {
    }

    public PipelineBuilder<T> addHandler(Handler<T> handler) {
        handlers.add(handler);
        return this;
    }
    
    public PipelineBuilder<T> onError(ErrorHandler<T> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public Pipeline<T> build() {
        return new Pipeline<>(handlers, errorHandler);
    }

}
