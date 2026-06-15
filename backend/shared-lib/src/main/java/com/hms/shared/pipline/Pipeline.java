package com.hms.shared.pipline;

import java.util.List;

public class Pipeline<T> implements Handler<T> {

    private final List<Handler<T>> handlers;
    private final ErrorHandler<T> errorHandler;

    protected Pipeline(List<Handler<T>> handlers, ErrorHandler<T> errorHandler) {
        this.handlers = handlers;
        this.errorHandler = errorHandler;
    }

    public static <T> PipelineBuilder<T> builder() {
        return new PipelineBuilder<>();

    }

    @Override
    public T handle(T entry) throws Exception {
        for (Handler<T> handler : handlers) {
            try {
                entry = handler.handle(entry);
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.handleError(entry, e);
                } else {
                    throw e;
                }
            }
        }
        return entry;
    }
    

}
