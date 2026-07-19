package com.hms.stream.torrentsession;

import java.time.Duration;
import java.time.Instant;
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
import java.util.function.Function;
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
import com.frostwire.jlibtorrent.alerts.DhtErrorAlert;
import com.frostwire.jlibtorrent.alerts.FileErrorAlert;
import com.frostwire.jlibtorrent.alerts.FileRenameFailedAlert;
import com.frostwire.jlibtorrent.alerts.HashFailedAlert;
import com.frostwire.jlibtorrent.alerts.ListenFailedAlert;
import com.frostwire.jlibtorrent.alerts.LsdErrorAlert;
import com.frostwire.jlibtorrent.alerts.LsdPeerAlert;
import com.frostwire.jlibtorrent.alerts.MetadataFailedAlert;
import com.frostwire.jlibtorrent.alerts.PieceFinishedAlert;
import com.frostwire.jlibtorrent.alerts.PortmapErrorAlert;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataFailedAlert;
import com.frostwire.jlibtorrent.alerts.ScrapeFailedAlert;
import com.frostwire.jlibtorrent.alerts.SessionErrorAlert;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;
import com.frostwire.jlibtorrent.alerts.TorrentDeleteFailedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentErrorAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TrackerErrorAlert;
import com.frostwire.jlibtorrent.alerts.UdpErrorAlert;
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

    private final HashMap<Sha1Hash, TorrentHandle> torrentHandles = new
    HashMap<>();
    // private final InfoHashMap torrentHandles;

    private final DelegatingAlertListener delegatingAlertListener = new DelegatingAlertListener(
            List.of(
                    AlertHandler.of(AlertsDroppedAlert.class, alert -> LOG.warn("Alerts dropped: {}", alert.message())),
                    // AlertHandler.of(DhtErrorAlert.class, alert -> LOG.warn("DHT error: {} | Operation: {}", alert.message(), alert.operation())),
                    AlertHandler.of(ListenFailedAlert.class, alert -> LOG.warn("Listen failed: {}", alert.message())),
                    // AlertHandler.of(LsdPeerAlert.class, alert -> LOG.info("LSD peer: {}", alert.message())),
                    // AlertHandler.of(LsdErrorAlert.class, alert -> LOG.warn("LSD error: {}", alert.message())),
                    AlertHandler.of(PortmapErrorAlert.class, alert -> LOG.warn("Portmap error: {}", alert.message())),
                    AlertHandler.of(SessionErrorAlert.class, alert -> LOG.warn("Session error: {}", alert.message())),
                    AlertHandler.of(UdpErrorAlert.class, alert -> LOG.warn("UDP error: {}", alert.message()))));

    private final ThreadPoolTaskScheduler scheduler;
    private final ThreadPoolTaskExecutor awaiter;

    private TorrentSession() {
        sessionManager = new SessionManager();
        // torrentHandles = new InfoHashMap(sessionManager);

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("TorrentSession-scheduler-");
        scheduler.initialize();

        awaiter = new ThreadPoolTaskExecutor();
        awaiter.setCorePoolSize(1);
        awaiter.setMaxPoolSize(100);
        awaiter.setVirtualThreads(true);
        awaiter.setThreadNamePrefix("TorrentSession-awaiter-");
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
            if (sessionManager != null && sessionManager.isRunning() && !hasOpenTorrents()) {
                sessionManager.removeListener(delegatingAlertListener);
                sessionManager.stop();
                sessionHandle = null;
            }
        }
    }

    public boolean hasOpenTorrents() {
        return !torrentHandles.isEmpty();
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

    private final Map<Sha1Hash, ScheduledFuture<?>> saveResumeDataFutures = new HashMap<>();

    private final TorrentAlertHandler<?>[] defaultTorrentAlertHandlers = TorrentAlertHandler.arrayOf(
            TorrentAlertHandler.of(FileErrorAlert.class, alert -> LOG.warn("File error: {}", alert.message())),
            TorrentAlertHandler.of(FileRenameFailedAlert.class,
                    alert -> LOG.warn("File rename failed: {}", alert.message())),
            TorrentAlertHandler.of(HashFailedAlert.class, alert -> LOG.warn("Hash failed: {}", alert.message())),
            TorrentAlertHandler.of(MetadataFailedAlert.class,
                    alert -> LOG.warn("Metadata failed: {}", alert.message())),
            TorrentAlertHandler.of(SaveResumeDataFailedAlert.class,
                    alert -> LOG.warn("Save resume data failed: {}", alert.message())),
            TorrentAlertHandler.of(TorrentDeleteFailedAlert.class,
                    alert -> LOG.warn("Torrent delete failed: {}", alert.message())),
            TorrentAlertHandler.of(TorrentErrorAlert.class, alert -> LOG.warn("Torrent error: {}", alert.message())),
            TorrentAlertHandler.of(ScrapeFailedAlert.class, alert -> LOG.warn("Scrape failed: {}", alert.message())),
            TorrentAlertHandler.of(TrackerErrorAlert.class, alert -> LOG.warn("Tracker error: {}", alert.message())),
            new PieceDeadlineUpdater());

    public CompletableFuture<TorrentHandle> addTorrent(AddTorrentParams params, Consumer<TorrentHandle> onHandleInvalid, TorrentAlertHandler<?>... alertHandlers)
            throws TorrentException {
        CountDownLatch signal = new CountDownLatch(1);
        TorrentAlertListener listener = new TorrentAlertListener(params.infoHash(),
                Arrays.asList(
                        TorrentAlertHandler.join(
                                alertHandlers,
                                defaultTorrentAlertHandlers,
                                TorrentAlertHandler.arrayOf(
                                        TorrentAlertHandler.of(TorrentAlert.class, alert -> {
                                            TorrentHandle handle = alert.handle();
                                            if (handle == null || !handle.isValid()) {
                                                LOG.warn("Torrent handle is invalid or null for infoHash: {} alert: {}",
                                                        params.infoHash(), alert.message());
                                                onHandleInvalid.accept(handle);
                                                signal.countDown();
                                            }
                                        }),
                                        new TorrentFinishedAlertHandler(signal)))));
        addListener(listener);

        TorrentHandle handle = addTorrent(params);
        if (handle == null || !handle.isValid()) {
            throw new AddTorrentException("Failed to add torrent: Invalid handle");
        }

        ScheduledFuture<?> saveResumeDataTask = scheduler.scheduleAtFixedRate(() -> {
            if (handle == null || !handle.isValid()) {
                onHandleInvalid.accept(handle);
                signal.countDown();
                return;
            }
            handle.saveResumeData();
        }, Duration.ofSeconds(1));

        saveResumeDataFutures.put(handle.infoHash(), saveResumeDataTask);

        return CompletableFuture.runAsync(() -> {
            try {
                signal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                saveResumeDataTask.cancel(true);
                saveResumeDataFutures.remove(handle.infoHash());
                torrentHandles.remove(handle.infoHash());
                removeListener(listener);
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
        if (infoHash == null || infoHash.isEmpty()) {
            return null;
        }
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
        TorrentHandle handle = getTorrentHandle(infoHash);
        if (handle != null) {
            handle.resume();
            return true;
        }
        return false;
    }

    public boolean deleteTorrent(String infoHash) {
        TorrentHandle handle = getTorrentHandle(infoHash);
        if (handle != null) {
            handle.pause();
            removeListenerForHandle(handle);
            ScheduledFuture<?> saveResumeDataTask = saveResumeDataFutures.get(handle.infoHash());
            if (saveResumeDataTask != null) {
                saveResumeDataTask.cancel(true);
                saveResumeDataFutures.remove(handle.infoHash());
            }
            sessionHandle.removeTorrent(handle, SessionHandle.DELETE_FILES);
            torrentHandles.remove(new Sha1Hash(infoHash));
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

        public DelegatingAlertListener(List<AlertHandler<?>> alertHandlers) {
            this(alertHandlers, new ArrayList<>());
        }

        public DelegatingAlertListener(List<AlertHandler<?>> alertHandlers,
                List<TorrentAlertListener> torrentAlertListeners) {
            this.alertHandlers = alertHandlers;
            this.torrentAlertListeners = torrentAlertListeners.stream()
                    .map(tal -> Map.entry(tal.getTorrentHash(), tal))
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }

        private void addTorrentAlertListener(TorrentAlertListener listener) {
            torrentAlertListeners.put(listener.getTorrentHash(), listener);
        }

        private void removeTorrentAlertListener(TorrentAlertListener listener) {
            torrentAlertListeners.remove(listener.getTorrentHash());
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
            // LOG.info("Torrent download finished for: {}", alert.handle().name());
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

class InfoHashMap {
    private final HashMap<Sha1Hash, Function<Sha1Hash, TorrentHandle>> infoHashMap = new HashMap<>();
    private final SessionManager sessionManager;

    public InfoHashMap(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void add(Sha1Hash infoHash) {
        infoHashMap.put(infoHash, (hash) -> {
            return sessionManager.find(hash);
        });
    }

    public void remove(Sha1Hash infoHash) {
        infoHashMap.remove(infoHash);
    }

    public boolean contains(Sha1Hash infoHash) {
        return infoHashMap.containsKey(infoHash);
    }

    public int size() {
        return infoHashMap.size();
    }

    public void clear() {
        infoHashMap.clear();
    }

    public TorrentHandle get(Sha1Hash infoHash) {
        Function<Sha1Hash, TorrentHandle> func = infoHashMap.get(infoHash);
        TorrentHandle handle = func != null ? func.apply(infoHash) : null;
        if (handle == null || !handle.isValid()) {
            infoHashMap.remove(infoHash);
            return null;
        }
        return handle;
    }

    public List<Sha1Hash> getInfoHashes() {
        return new ArrayList<>(infoHashMap.keySet());
    }

    public List<TorrentHandle> getTorrentHandles() {
        return infoHashMap.keySet().stream()
                .map(this::get)
                .filter(handle -> handle != null && handle.isValid())
                .toList();
    }
}
