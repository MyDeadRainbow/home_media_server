package com.hms.stream.importmedia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.libtorrent4j.AlertListener;
import org.libtorrent4j.FileStorage;
import org.libtorrent4j.Priority;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentFlags;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;
import org.libtorrent4j.alerts.BlockFinishedAlert;
import org.libtorrent4j.alerts.TorrentAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hms.stream.importmedia.pipeline.ImportMediaHandler;

public class TorrentMagnetLink implements ImportMediaHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentMagnetLink.class);

    private static final Path TORRENT_DOWNLOAD_FOLDER = Path.of("torrents");
    private static final Path TEMP_FOLDER = Path.of("temp");

    public TorrentMagnetLink() {
        try {
            Files.createDirectories(TORRENT_DOWNLOAD_FOLDER);
            Files.createDirectories(TEMP_FOLDER);
        } catch (Exception e) {
            LOG.error("Failed to create torrent directory", e);
            return;
        }
    }

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {
        try {
            Files.createDirectories(TORRENT_DOWNLOAD_FOLDER);
            Files.createDirectories(TEMP_FOLDER);
        } catch (Exception e) {
            LOG.error("Failed to create torrent directory", e);
            return entry;
        }
        String magnetLink = entry.magnetLink();
        if (magnetLink == null || magnetLink.isEmpty()) {
            LOG.error("Magnet link is null or empty for entry: {}", entry);
            entry = entry.withStatus(ImportMediaStatus.MAGNET_NOT_FOUND);
            try {
                entry.update();
            } catch (Exception e) {
                LOG.error("Failed to update entry status to MAGNET_NOT_FOUND", e);
            }
            return entry;
        }
        // libtorrent4j does not work in docker container due to missing dependencies.
        // Will need to switch to a different torrent library or implement torrent
        // downloading in a separate service that runs on the host machine and can
        // access the torrent download directory directly.
        final SessionManager s = new SessionManager();

        final CountDownLatch signal = new CountDownLatch(1);

        final ImportMediaAlertListener listener = new ImportMediaAlertListener(entry, signal);
        s.addListener(listener);

        s.start();

        byte[] magnetData = null;
        for (int i = 0; i < 3; i++) {
            magnetData = s.fetchMagnet(magnetLink, 30, TEMP_FOLDER.toFile());
            if (magnetData != null) {
                break;
            }
        }
        if (magnetData == null) {
            LOG.error("Failed to fetch magnet data after 3 attempts");
            entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
            try {
                entry.update();
            } catch (Exception e) {
                LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
            }
            return entry;
        }

        Path torrentDestination = null;

        TorrentInfo info = new TorrentInfo(magnetData);
        FileStorage storage = info.files();

        Priority[] priorities = Priority.array(Priority.IGNORE, info.numFiles());
        for (int i = 0; i < priorities.length; i++) {
            String filePath = storage.filePath(i);
            Path path = Path.of(filePath);
            
            if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                    filePath.endsWith(".avi")) {
                priorities[i] = Priority.DEFAULT;
            }
        }
        s.download(info, TORRENT_DOWNLOAD_FOLDER.toFile(), null, priorities, null,
                TorrentFlags.SEQUENTIAL_DOWNLOAD);

        try {
            signal.await();
        } catch (InterruptedException e) {
            LOG.error("Torrent download interrupted", e);
            Thread.currentThread().interrupt();
        }

        s.stop();
        return listener.getUpdatedEntry();
    }

    class ImportMediaAlertListener implements AlertListener {
        private final ImportMediaEntry entry;
        private ImportMediaEntry updatedEntry;
        private int lastLoggedProgress = -1;
        private final CountDownLatch signal;

        public ImportMediaAlertListener(ImportMediaEntry entry, CountDownLatch signal) {
            this.entry = entry;
            this.updatedEntry = entry;
            this.signal = signal;
        }

        public ImportMediaEntry getUpdatedEntry() {
            return updatedEntry;
        }

        @Override
        public int[] types() {
            return null; // Listen to all alert types
        }

        @Override
        public void alert(Alert<?> alert) {
            AlertType type = alert.type();

            switch (type) {
                case ADD_TORRENT:
                    LOG.info("Torrent added");
                    ((AddTorrentAlert) alert).handle().resume();
                    break;
                case BLOCK_FINISHED:
                    BlockFinishedAlert a = (BlockFinishedAlert) alert;
                    int p = (int) (a.handle().status().progress() * 100);
                    if (p % 10 == 0 && p != lastLoggedProgress) {
                        LOG.info("Progress: {}% for torrent name: {}", p, a.handle().getName());
                        lastLoggedProgress = p;
                    }
                    break;
                case TORRENT_FINISHED:
                    LOG.info("Torrent download finished for: {}",
                            ((org.libtorrent4j.alerts.TorrentFinishedAlert) alert).handle().getName());
                    updatedEntry = entry.withStatus(ImportMediaStatus.TORRENT_DOWNLOADED);
                    try {
                        updatedEntry.update();
                    } catch (Exception e) {
                        LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
                    }
                    signal.countDown();
                    break;
                default:
                    // if (alert instanceof TorrentAlert) {
                    //     LOG.info("Received alert: {} for torrent: {}", type,
                    //             ((TorrentAlert<?>) alert).handle().getName());
                    // } else {
                    //     LOG.info("Received alert: {}", type);
                    // }
                    break;
            }
        }
    }
}
