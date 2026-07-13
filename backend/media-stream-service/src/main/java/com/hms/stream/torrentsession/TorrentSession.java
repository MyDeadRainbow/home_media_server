package com.hms.stream.torrentsession;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.frostwire.jlibtorrent.AddTorrentParams;
import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.ErrorCode;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.PeerRequest;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.SessionHandle;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertsDroppedAlert;
import com.frostwire.jlibtorrent.alerts.PieceFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.swig.error_code;
import com.hms.stream.torrentsession.exception.AddTorrentException;
import com.hms.stream.torrentsession.exception.TorrentException;

public class TorrentSession implements AutoCloseable {
    private static final TorrentSession INSTANCE = new TorrentSession();

    private static final Logger LOG = LoggerFactory.getLogger(TorrentSession.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int PIECE_DEADLINE_BLOCK_LENGTH = 1024; // 16 KB

    private SessionManager sessionManager;
    private SessionHandle sessionHandle;
    private AtomicInteger sessionCount = new AtomicInteger(0);

    private final HashMap<Sha1Hash, TorrentHandle> torrentHandles = new HashMap<>();

    private final DelegatingAlertListener delegatingAlertListener = new DelegatingAlertListener(
            List.of(AlertHandler.of(AlertsDroppedAlert.class, (AlertsDroppedAlert alert) -> {
                // Handle the alert
            })),
            new ArrayList<>());

    private final ThreadPoolTaskScheduler scheduler;
    private final ThreadPoolTaskExecutor awaiter;

    private TorrentSession() {
        sessionManager = new SessionManager();

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setVirtualThreads(true);
        scheduler.initialize();

        awaiter = new ThreadPoolTaskExecutor();
        awaiter.setCorePoolSize(1);
        awaiter.setMaxPoolSize(1);
        awaiter.setQueueCapacity(1);
        awaiter.setVirtualThreads(true);
        awaiter.setThreadNamePrefix("awaiter-");
        awaiter.initialize();
    }

    public static TorrentSession getInstance() {
        if (!INSTANCE.sessionManager.isRunning()) {
            // Add the delegating alert listener to the session manager before start
            INSTANCE.sessionManager.addListener(INSTANCE.delegatingAlertListener);

            INSTANCE.sessionManager.start();

            // initialize the handle after it is started
            INSTANCE.sessionHandle = new SessionHandle(INSTANCE.sessionManager.swig());
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
                sessionManager.removeListener(delegatingAlertListener);
                sessionManager.stop();
                sessionHandle = null;
            }
        }
    }

    public byte[] fetchMagnet(String magnetLink, int timeoutSeconds) {
        return sessionManager.fetchMagnet(magnetLink, timeoutSeconds);
    }

    public void addListener(TorrentAlertListener listener) {
        delegatingAlertListener.addTorrentAlertListener(listener);
    }

    public void removeListener(TorrentAlertListener listener) {
        delegatingAlertListener.removeTorrentAlertListener(listener);
    }

    public TorrentHandle addTorrent(AddTorrentParams params) throws TorrentException {
        ErrorCode errorCode = newErrorCode();
        TorrentHandle handle = sessionHandle.addTorrent(params, errorCode);
        if (errorCode.value() != 0) {
            throw new AddTorrentException("Failed to add torrent: " + errorCode.message());
        }
        torrentHandles.put(handle.infoHash(), handle);
        return handle;
    }

    public CompletableFuture<TorrentHandle> addTorrent(AddTorrentParams params, TorrentAlertHandler<?>... alertHandlers)
            throws TorrentException {
        CountDownLatch signal = new CountDownLatch(1);
        TorrentHandle handle = addTorrent(params);
        TorrentAlertListener listener = new TorrentAlertListener(handle,
                Arrays.asList(
                        TorrentAlertHandler.join(
                                alertHandlers,
                                TorrentAlertHandler.arrayOf(new PieceDeadlineUpdater(),
                                        new TorrentFinishedAlertHandler(signal)))));
        addListener(listener);

        ScheduledFuture<?> saveResumeDataTask = scheduler.scheduleAtFixedRate(() -> {
            handle.saveResumeData();
        }, Duration.ofSeconds(1));

        return CompletableFuture.runAsync(() -> {
            try {
                signal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                saveResumeDataTask.cancel(true);
            }
        }, awaiter).thenApply(v -> handle);
    }

    public void removeListenerForHandle(TorrentHandle handle) {
        Sha1Hash infoHash = handle.infoHash();
        TorrentAlertListener listenerToRemove = delegatingAlertListener.torrentAlertListeners.get(infoHash);
        if (listenerToRemove != null) {
            removeListener(listenerToRemove);
        }
    }

    public List<TorrentHandle> getTorrentHandles() {
        return new ArrayList<>(torrentHandles.values());
    }

    public TorrentHandle getTorrentHandle(String infoHash) {
        return torrentHandles.get(new Sha1Hash(infoHash));
    }

    public boolean pauseTorrent(String infoHash) {
        TorrentHandle handle = getTorrentHandle(infoHash);
        if (handle != null) {
            handle.pause();
            return true;
        }
        return false;
    }

    public boolean resumeTorrent(String infoHash) {
        TorrentHandle handle = torrentHandles.get(new Sha1Hash(infoHash));
        if (handle != null) {
            handle.resume();
            return true;
        }
        return false;
    }

    public boolean deleteTorrent(String infoHash) {
        TorrentHandle handle = torrentHandles.get(new Sha1Hash(infoHash));
        if (handle != null) {
            handle.pause();
            removeListenerForHandle(handle);
            sessionHandle.removeTorrent(handle, SessionHandle.DELETE_FILES);
            torrentHandles.remove(handle.infoHash());
            return true;
        }
        return false;
    }

    // Utility section
    private ErrorCode newErrorCode() {
        return new ErrorCode(new error_code());
    }

    private class DelegatingAlertListener implements AlertListener {
        private final List<AlertHandler<?>> alertHandlers;
        private final Map<Sha1Hash, TorrentAlertListener> torrentAlertListeners;

        public DelegatingAlertListener(List<AlertHandler<?>> alertHandlers,
                List<TorrentAlertListener> torrentAlertListeners) {
            this.alertHandlers = alertHandlers;
            this.torrentAlertListeners = torrentAlertListeners.stream()
                    .map(tal -> Map.entry(tal.getTorrentHandle().infoHash(), tal))
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }

        private void addTorrentAlertListener(TorrentAlertListener listener) {
            torrentAlertListeners.put(listener.getTorrentHandle().infoHash(), listener);
        }

        private void removeTorrentAlertListener(TorrentAlertListener listener) {
            torrentAlertListeners.remove(listener.getTorrentHandle().infoHash());
        }

        @SuppressWarnings("unchecked")
        @Override
        public void alert(Alert<?> alert) {
            switch (alert) {
                case TorrentAlert<?> ta -> {
                    // Ensure the alert is associated with a torrent handle
                    TorrentAlertListener listener = torrentAlertListeners.get(ta.handle().infoHash());
                    if (listener != null) {
                        listener.alert(ta);
                    }
                    break;
                }
                case Alert<?> a -> {
                    for (AlertHandler<?> handler : alertHandlers) {
                        if (handler.type.isAssignableFrom(alert.getClass())) {
                            ((AlertHandler<Alert<?>>) handler).handle(a);
                        }
                    }
                }
            }

        }

        @Override
        public int[] types() {
            return null; // Listen to all alert types
        }
    }

    class TorrentFinishedAlertHandler extends TorrentAlertHandler<TorrentFinishedAlert> {

        private final CountDownLatch signal;

        public TorrentFinishedAlertHandler(CountDownLatch signal) {
            super(TorrentFinishedAlert.class);
            this.signal = signal;
        }

        @Override
        public void handle(TorrentFinishedAlert alert) {
            LOG.info("Torrent download finished for: {}", alert.handle().name());
            // entry = entry.withStatus(ImportMediaStatus.TORRENT_DOWNLOADED);
            // try {
            // new ImportMediaEntry.Dao().update(entry);
            // } catch (SQLException e) {
            // LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
            // }
            // signal.countDown();
            signal.countDown();
        }
    }

    class PieceDeadlineUpdater extends TorrentAlertHandler<PieceFinishedAlert> {

        int currentPieceIndex = -1;
        int currentFileIndex = -1;
        int nextFileIndex = -1;

        public PieceDeadlineUpdater() {
            super(PieceFinishedAlert.class);
        }

        @Override
        public void handle(PieceFinishedAlert alert) {
            TorrentHandle handle = alert.handle();
            int pieceIndex = alert.pieceIndex();
            Priority[] filePriorities = handle.filePriorities();
            FileStorage storage = handle.torrentFile().files();
            TorrentInfo info = handle.torrentFile();

            // List<FileSlice> fileSlices = info.mapBlock(pieceIndex, 0,
            // info.pieceSize(pieceIndex));
            // int fileIndex = fileSlices.get(0).fileIndex();

            boolean isCurrentFileDone = false;
            // start off by setting deadlines for the first file for the first
            // PIECE_DEADLINE_BLOCK_LENGTH
            if (currentFileIndex == -1) {
                for (int i = 0; i < filePriorities.length; i++) {
                    int fileSize = (int) storage.fileSize(i);
                    if (fileSize == 0 || filePriorities[i] == Priority.IGNORE) {
                        // LOG.info("File size is 0 for file: {} in torrent: {}", storage.filePath(i),
                        // info.name());
                        continue;
                    }

                    PeerRequest pr = info.mapFile(i, 0, fileSize);
                    int startIndex = pr.piece();

                    int numPieces = pr.length();
                    int endIndex = startIndex + numPieces - 1;

                    int deadline = 1;
                    int count = 0;
                    for (int j = startIndex; j <= endIndex; j++) {
                        if (count == PIECE_DEADLINE_BLOCK_LENGTH) {
                            currentPieceIndex = j;
                            break;
                        }
                        count++;
                        handle.setPieceDeadline(j, deadline);
                    }
                    currentFileIndex = i;
                    nextFileIndex = i;
                    break; // Only set deadlines for the first valid file
                }

                // then if the next file index is still the current file index, the currentFile
                // is not done.
                // set the next PIECE_DEADLINE_BLOCK_LENGTH pieces for the current file
            } else if (nextFileIndex == currentFileIndex) {
                int fileSize = (int) storage.fileSize(currentFileIndex);
                PeerRequest pr = info.mapFile(currentFileIndex, 0, fileSize);
                int startIndex = pr.piece();

                int numPieces = pr.length();
                int endIndex = startIndex + numPieces - 1;
                if (pieceIndex == currentPieceIndex) {
                    int deadline = 1;
                    int count = 0;
                    for (int j = currentPieceIndex + 1; j < currentPieceIndex + 1 + PIECE_DEADLINE_BLOCK_LENGTH; j++) {
                        if (j >= endIndex) {
                            isCurrentFileDone = true;
                            break;
                        }
                        count++;
                        handle.setPieceDeadline(j, deadline);
                        currentPieceIndex = j;
                    }
                }
            } else {
                currentFileIndex = nextFileIndex;
                int fileSize = (int) storage.fileSize(currentFileIndex);
                PeerRequest pr = info.mapFile(currentFileIndex, 0, fileSize);
                int startIndex = pr.piece();

                int numPieces = pr.length();
                int endIndex = startIndex + numPieces - 1;
                if (pieceIndex == currentPieceIndex) {
                    int deadline = 1;
                    int count = 0;
                    for (int j = currentPieceIndex + 1; j < currentPieceIndex + 1 + PIECE_DEADLINE_BLOCK_LENGTH; j++) {
                        if (j >= endIndex) {
                            isCurrentFileDone = true;
                            break;
                        }
                        count++;
                        handle.setPieceDeadline(j, deadline);
                        currentPieceIndex = j;
                    }
                }
            }
            // if the next file index is not the current file index, the current file is
            // done.
            // set the next PIECE_DEADLINE_BLOCK_LENGTH pieces for the next file
            if (isCurrentFileDone) {
                for (int i = currentFileIndex + 1; i < filePriorities.length; i++) {
                    int nextFileSize = (int) storage.fileSize(i);
                    if (nextFileSize == 0 || filePriorities[i] == Priority.IGNORE) {
                        continue;
                    }

                    nextFileIndex = i;
                    // currentFileIndex = i;
                    // PeerRequest pr = info.mapFile(i, 0, nextFileSize);
                    // int startIndex = pr.piece();

                    // int numPieces = pr.length();
                    // int endIndex = startIndex + numPieces - 1;

                    // int deadline = 1;
                    // int count = 0;
                    // for (int j = startIndex; j <= endIndex; j++) {
                    // if (count == PIECE_DEADLINE_BLOCK_LENGTH) {
                    // break;
                    // }
                    // handle.setPieceDeadline(j, deadline++);
                    // count++;
                    // }
                    // currentPieceIndex = startIndex + count - 1;
                    break;
                }
            }
        }

    }
}
