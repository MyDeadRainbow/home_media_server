package com.hms.stream.torrentsession;

import java.util.function.Consumer;

import com.frostwire.jlibtorrent.alerts.TorrentAlert;

public abstract class TorrentAlertHandler<T extends TorrentAlert<?>> {//extends AlertHandler<T> {
    public final Class<T> type;
    public TorrentAlertHandler(Class<T> type) {
        this.type = type;
    }

    public abstract void handle(T alert);

    public static <T extends TorrentAlert<?>> TorrentAlertHandler<T> of(Class<T> type, Consumer<T> handler) {
        return new TorrentAlertHandler<T>(type) {
            @Override
            public void handle(T alert) {
                handler.accept(alert);
            }
        };
    }

    public static TorrentAlertHandler<?>[] arrayOf(TorrentAlertHandler<?>... handlers) {
        return handlers;
    }

    public static TorrentAlertHandler<?>[] join(TorrentAlertHandler<?>[]... handlerArrays) {
        int totalLength = 0;
        for (TorrentAlertHandler<?>[] array : handlerArrays) {
            totalLength += array.length;
        }
        TorrentAlertHandler<?>[] result = new TorrentAlertHandler<?>[totalLength];
        int currentIndex = 0;
        for (TorrentAlertHandler<?>[] array : handlerArrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        return result;
    }
}
