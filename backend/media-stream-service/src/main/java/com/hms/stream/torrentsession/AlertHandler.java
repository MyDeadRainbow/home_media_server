package com.hms.stream.torrentsession;

import java.util.function.Consumer;

import com.frostwire.jlibtorrent.alerts.Alert;

public abstract class AlertHandler<T extends Alert<?>> {
    public final Class<T> type;

    public abstract void handle(T alert);

    public AlertHandler(Class<T> type) {
        this.type = type;
    }

    public static <T extends Alert<?>> AlertHandler<T> of(Class<T> type, Consumer<T> handler) {
        return new AlertHandler<T>(type) {
            @Override
            public void handle(T alert) {
                handler.accept(alert);
            }
        };
    }
}
