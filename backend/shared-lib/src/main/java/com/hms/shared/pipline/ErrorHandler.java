package com.hms.shared.pipline;

@FunctionalInterface
public interface ErrorHandler<T> {
    void handleError(T entry, Exception e);
}
