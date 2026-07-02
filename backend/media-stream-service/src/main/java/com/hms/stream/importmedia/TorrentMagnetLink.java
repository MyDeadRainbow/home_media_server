package com.hms.stream.importmedia;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

import com.hms.dao.DBFileNotFoundException;
import com.hms.dao.GetConnectionException;
import com.hms.shared.media.MediaItem;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;
import com.hms.stream.importmedia.pipeline.ImportMediaHandler;
import com.hms.stream.messaging.CatalogUpdateProducer;

public class TorrentMagnetLink implements ImportMediaHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentMagnetLink.class);

    // private static final Path TORRENT_DOWNLOAD_FOLDER = Path.of("torrents");
    protected static final Path moviesRoot = Paths.get("media", "movies");
    protected static final Path seriesRoot = Paths.get("media", "series");
    protected static final Path TEMP_FOLDER = Path.of("temp");

    public TorrentMagnetLink() {
    }

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {
        // try {
        // Files.createDirectories(TEMP_FOLDER);
        // } catch (Exception e) {
        // LOG.error("Failed to create torrent directory", e);
        // return entry;
        // }

        String magnetLink = entry.magnetLink();
        if (magnetLink == null || magnetLink.isEmpty()) {
            LOG.error("Magnet link is null or empty for entry: {}", entry);
            entry = entry.withStatus(ImportMediaStatus.MAGNET_NOT_FOUND);
            try {
                new ImportMediaEntry.Dao().update(entry);
            } catch (Exception e) {
                LOG.error("Failed to update entry status to MAGNET_NOT_FOUND", e);
            }
            return entry;
        }

        Path destinationFolder = switch (entry.category()) {
            case MOVIE -> moviesRoot;
            case SERIES -> seriesRoot;
            default -> throw new IllegalArgumentException("Unsupported media category: " + entry.category());
        };

        File resumeFile = entry.resumeFile() != null ? new File(entry.resumeFile())
                : destinationFolder.resolve("resume").resolve(entry.id() + ".resume").toFile();
        entry = entry.withResumeFile(resumeFile.getAbsolutePath());
        try {
            new ImportMediaEntry.Dao().update(entry);
        } catch (SQLException e) {
            LOG.error("Failed to update entry with resume file path", e);
        }
        // libtorrent4j does not work in docker container due to missing dependencies.
        // Will need to switch to a different torrent library or implement torrent
        // downloading in a separate service that runs on the host machine and can
        // access the torrent download directory directly.
        final CountDownLatch signal = new CountDownLatch(1);
        final ImportMediaAlertListener listener = new ImportMediaAlertListener(entry, signal);
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            SessionManager s = torrentSession.getSessionManager();

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
                    new ImportMediaEntry.Dao().update(entry);
                } catch (Exception e) {
                    LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                }
                return entry;
            }

            TorrentInfo info = new TorrentInfo(magnetData);
            FileStorage storage = info.files();

            List<FilePathRecord> filePathRecords = new ArrayList<>();

            Priority[] priorities = Priority.array(Priority.IGNORE, info.numFiles());
            for (int i = 0; i < priorities.length; i++) {
                String filePath = storage.filePath(i);
                Path path = Path.of(filePath);

                if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                        filePath.endsWith(".avi")) {
                    priorities[i] = Priority.DEFAULT;

                    MediaItem record = new MediaItem(UUID.randomUUID().toString(),
                            destinationFolder.resolve(path).toAbsolutePath().toString());

                    try {
                        new MediaItem.Dao().insert(record);
                    } catch (Exception e) {
                        LOG.error("Failed to insert media record for file: {}", path, e);
                    }

                    filePathRecords.add(new FilePathRecord(record.mediaId(), path.getFileName().toString()));

                }
            }

            CatalogUpdateProducer.postMessage(new CatalogUpdate(CatalogUpdateType.CREATED,
                    entry.category(), filePathRecords));

            s.download(info, destinationFolder.toFile(), resumeFile, priorities, null,
                    TorrentFlags.SEQUENTIAL_DOWNLOAD);

            try {
                signal.await();
            } catch (InterruptedException e) {
                LOG.error("Torrent download interrupted", e);
                Thread.currentThread().interrupt();
            }

        } finally {
            signal.countDown();
        }
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
                case ADD_TORRENT: {
                    LOG.info("Torrent added");
                    updatedEntry = updatedEntry.withStatus(ImportMediaStatus.IN_PROGRESS);
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntry);
                    } catch (SQLException e) {
                        LOG.error("Failed to update entry status to IN_PROGRESS", e);
                    }
                    // ((AddTorrentAlert) alert).handle().resume();
                    break;
                }
                case BLOCK_FINISHED: {
                    BlockFinishedAlert a = (BlockFinishedAlert) alert;
                    int p = (int) (a.handle().status().progress() * 100);
                    if (p % 10 == 0 && p != lastLoggedProgress) {
                        LOG.info("Progress: {}% for torrent name: {}", p, a.handle().getName());
                        lastLoggedProgress = p;
                    }
                    break;
                }
                case TORRENT_FINISHED: {
                    LOG.info("Torrent download finished for: {}",
                            ((org.libtorrent4j.alerts.TorrentFinishedAlert) alert).handle().getName());
                    updatedEntry = updatedEntry.withStatus(ImportMediaStatus.TORRENT_DOWNLOADED);
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntry);
                    } catch (SQLException e) {
                        LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
                    }
                    signal.countDown();
                    break;
                }
                // case FILE_PROGRESS: {
                // org.libtorrent4j.alerts.FileProgressAlert a =
                // (org.libtorrent4j.alerts.FileProgressAlert) alert;
                // long[] files = a.getFiles();
                // // a.handle().torrentFile().files().filePath();
                // // int index = a.index();
                // // long bytesDone = a.bytesDone();
                // // long totalBytes = a.fileSize();
                // // int progress = (int) ((bytesDone * 100) / totalBytes);
                // // if (progress % 10 == 0 && progress != lastLoggedProgress) {
                // // LOG.info("File progress: {}% for file: {} in torrent: {}", progress,
                // // a.handle().status().fileName(index), a.handle().getName());
                // // lastLoggedProgress = progress;
                // // }
                // break;
                // }
                case FILE_COMPLETED: {
                    org.libtorrent4j.alerts.FileCompletedAlert a = (org.libtorrent4j.alerts.FileCompletedAlert) alert;
                    // LOG.info("File completed: {} in torrent: {}",
                    // a.handle().status().fileName(a.index()),
                    // a.handle().getName());
                    break;
                }
                default:
                    // if (alert instanceof TorrentAlert) {
                    // LOG.info("Received alert: {} for torrent: {}", type,
                    // ((TorrentAlert<?>) alert).handle().getName());
                    // } else {
                    // LOG.info("Received alert: {}", type);
                    // }
                    break;
            }
        }
    }
}
