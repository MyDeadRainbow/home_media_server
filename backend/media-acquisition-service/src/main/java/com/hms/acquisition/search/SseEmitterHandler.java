package com.hms.acquisition.search;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@FunctionalInterface
public interface SseEmitterHandler<T> {
    public abstract void handle(SseEmitter emitter, T data) throws Exception;
}
