package com.hms.stream.importmedia;

import java.util.concurrent.atomic.AtomicInteger;

import com.frostwire.jlibtorrent.SessionManager;


public class TorrentSession implements AutoCloseable {
    private static final TorrentSession INSTANCE = new TorrentSession();

    private SessionManager sessionManager;
    private AtomicInteger sessionCount = new AtomicInteger(0);

    private TorrentSession() {
        sessionManager = new SessionManager();
    }

    public static TorrentSession getInstance() {
        if (!INSTANCE.sessionManager.isRunning()) {
            INSTANCE.sessionManager.start();
        }
        INSTANCE.sessionCount.incrementAndGet();
        return INSTANCE;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void close() {
        if (sessionCount.decrementAndGet() == 0) {
            if (sessionManager != null && sessionManager.isRunning()) {
                sessionManager.stop();
            }
        }
    }
}
