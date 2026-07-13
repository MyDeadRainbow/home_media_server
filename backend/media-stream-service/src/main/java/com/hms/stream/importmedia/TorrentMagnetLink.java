package com.hms.stream.importmedia;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.frostwire.jlibtorrent.AddTorrentParams;
import com.frostwire.jlibtorrent.ErrorCode;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.PeerRequest;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentFlags;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Vectors;
import com.frostwire.jlibtorrent.alerts.AbstractAlert;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.FileCompletedAlert;
import com.frostwire.jlibtorrent.alerts.PieceFinishedAlert;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.swig.add_torrent_params;
import com.frostwire.jlibtorrent.swig.error_code;
import com.frostwire.jlibtorrent.swig.torrent_alert;
import com.hms.shared.media.MediaItem;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;
import com.hms.shared.util.Wrapper;
import com.hms.stream.importmedia.pipeline.ImportMediaHandler;
import com.hms.stream.messaging.CatalogUpdateProducer;
import com.hms.stream.torrentsession.TorrentAlertHandler;
import com.hms.stream.torrentsession.TorrentAlertListener;
import com.hms.stream.torrentsession.TorrentSession;
import com.hms.stream.torrentsession.exception.TorrentException;

public class TorrentMagnetLink implements ImportMediaHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentMagnetLink.class);

    protected static final Path moviesRoot = Paths.get("media", "movies");
    protected static final Path seriesRoot = Paths.get("media", "series");
    protected static final Path TEMP_FOLDER = Path.of("temp");
    protected static final Path RESUME_FOLDER = Path.of("resume");

    private final ThreadPoolTaskScheduler scheduler;

    public TorrentMagnetLink() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setVirtualThreads(true);
        scheduler.initialize();
    }

    public ImportMediaEntry ensureResumeFileExists(ImportMediaEntry entry, Path destinationFolder) {
        File resumeFile = entry.resumeFile() != null ? new File(entry.resumeFile())
                : destinationFolder.resolve(RESUME_FOLDER).resolve(entry.id() + ".resume").toFile();
        try {
            Files.createDirectories(resumeFile.getParentFile().toPath());
            if (!resumeFile.exists()) {
                resumeFile.createNewFile();
            }
        } catch (Exception e) {
            LOG.error("Failed to create resume file directory", e);
        }
        entry = entry.withResumeFile(resumeFile.getAbsolutePath());
        try {
            new ImportMediaEntry.Dao().update(entry);
        } catch (SQLException e) {
            LOG.error("Failed to update entry with resume file path", e);
        }
        return entry;
    }

    public ImportMediaEntry ensureMagnetDataFileExists(ImportMediaEntry entry, Path destinationFolder) {
        File magnetDataFile = entry.magnetDataFile() != null ? new File(entry.magnetDataFile())
                : destinationFolder.resolve(RESUME_FOLDER).resolve(entry.id() + ".magnet").toFile();
        try {
            Files.createDirectories(magnetDataFile.getParentFile().toPath());
            if (!magnetDataFile.exists()) {
                magnetDataFile.createNewFile();
            }
        } catch (Exception e) {
            LOG.error("Failed to create magnet data file directory", e);
        }
        entry = entry.withMagnetDataFile(magnetDataFile.getAbsolutePath());
        try {
            new ImportMediaEntry.Dao().update(entry);
        } catch (SQLException e) {
            LOG.error("Failed to update entry with magnet data file path", e);
        }
        return entry;
    }

    public static void main(String[] args) {
        TorrentHandle handle = null; // Assume this is initialized properly
        TorrentAlertListener listener = new TorrentAlertListener(handle,
                Arrays.asList(
                        TorrentAlertHandler.arrayOf(
                                TorrentAlertHandler.of(AddTorrentAlert.class, a -> {
                                    LOG.info("Torrent added: {}", a.handle().name());
                                    // updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.IN_PROGRESS));
                                    // var updatedEntry = updatedEntryWrapper.getValue();
                                    // try {
                                    // new ImportMediaEntry.Dao().update(updatedEntry);
                                    // } catch (SQLException e) {
                                    // LOG.error("Failed to update entry status to IN_PROGRESS", e);
                                    // }
                                }),
                                TorrentAlertHandler.of(SaveResumeDataAlert.class, a -> {
                                    byte[] resumeData = Vectors
                                            .byte_vector2bytes(
                                                    add_torrent_params.write_resume_data_buf(a.params().swig()));
                                    // File resumeFile = new File(updatedEntryWrapper.getValue().resumeFile());
                                    // try {
                                    // Files.write(resumeFile.toPath(), resumeData, StandardOpenOption.CREATE,
                                    // StandardOpenOption.TRUNCATE_EXISTING);
                                    // } catch (Exception e) {
                                    // LOG.error("Failed to write resume data to file: {}",
                                    // resumeFile.getAbsolutePath(), e);
                                    // }
                                }),
                                TorrentAlertHandler.of(FileCompletedAlert.class, a -> {
                                    int index = a.index();
                                    // TorrentHandle alertHandle = a.handle();
                                    // TorrentInfo alertInfo = a.handle().torrentFile();
                                    // FileStorage alertStorage = a.handle().torrentFile().files();
                                    // Priority[] priorities = Priority.array(Priority.IGNORE,
                                    // alertStorage.numFiles());

                                    // for (int i = 0; i < priorities.length; i++) {
                                    // String filePath = alertStorage.filePath(i);
                                    // if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                                    // filePath.endsWith(".avi")) {
                                    // if (index + 1 != i) {
                                    // priorities[i] = Priority.NORMAL;
                                    // } else {
                                    // priorities[i] = Priority.SEVEN;
                                    // }
                                    // }
                                    // }
                                    // updatePriorities(alertHandle, priorities);
                                    // setPieceDeadlinesForFile(alertHandle, alertInfo, alertStorage, index);
                                }),
                                TorrentAlertHandler.of(BlockFinishedAlert.class, a -> {
                                    // int p = (int) (a.handle().status().progress() * 100);
                                    // if (p % 10 == 0 && p != lastLoggedProgressWrapper.getValue()) {
                                    // LOG.info("Progress: {}% for torrent name: {}", p, a.handle().name());
                                    // lastLoggedProgressWrapper.setValue(p);
                                    // }
                                }))));
        try (TorrentSession s = TorrentSession.getInstance()) {
            s.addListener(listener);
        } catch (Exception e) {
            LOG.error("Failed to add listener for torrent handle", e);
        }
        // listener.alert();
    }

    // class TorrentAlertTester<T extends torrent_alert> extends AbstractAlert<T> {

    // TorrentAlertTester(T alert) {
    // super(alert);
    // //TODO Auto-generated constructor stub
    // }

    // // TorrentAlertTester(T alert) {
    // // // super(alert);
    // // }

    // }

    @Override
    public ImportMediaEntry handle(final ImportMediaEntry entry) {

        final Wrapper<ImportMediaEntry> updatedEntryWrapper = new Wrapper<>(entry);

        String magnetLink = entry.magnetLink();
        if (magnetLink == null || magnetLink.isEmpty()) {
            LOG.error("Magnet link is null or empty for entry: {}", updatedEntryWrapper.getValue());
            updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_NOT_FOUND));
            try {
                new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
            } catch (Exception e) {
                LOG.error("Failed to update entry status to MAGNET_NOT_FOUND", e);
            }
            return updatedEntryWrapper.getValue();
        }

        Path destinationFolder = switch (entry.category()) {
            case MOVIE -> moviesRoot;
            case SERIES -> seriesRoot;
            default -> throw new IllegalArgumentException("Unsupported media category: " + entry.category());
        };

        // libtorrent4j does not work in docker container due to missing dependencies.
        // Will need to switch to a different torrent library or implement torrent
        // downloading in a separate service that runs on the host machine and can
        // access the torrent download directory directly.

        // ScheduledFuture<?> saveResumeDataTask = null;
        // TorrentHandle handle;
        try (TorrentSession s = TorrentSession.getInstance()) {
            // SessionManager s = torrentSession.getSessionManager();
            // SessionHandle sh = new SessionHandle(s.swig());

            updatedEntryWrapper.apply(e -> ensureResumeFileExists(e, destinationFolder));
            updatedEntryWrapper.apply(e -> ensureMagnetDataFileExists(e, destinationFolder));

            byte[] magnetData = null;
            if (updatedEntryWrapper.getValue().magnetDataFile() != null
                    && updatedEntryWrapper.getValue().status() != ImportMediaStatus.QUEUED
                    && updatedEntryWrapper.getValue().status() != ImportMediaStatus.PENDING) {
                File magnetDataFile = new File(updatedEntryWrapper.getValue().magnetDataFile());
                if (magnetDataFile.exists()) {
                    try {
                        magnetData = Files.readAllBytes(magnetDataFile.toPath());
                    } catch (Exception e) {
                        LOG.error("Failed to read magnet data from file: {}", magnetDataFile.getAbsolutePath(), e);
                    }
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    magnetData = s.fetchMagnet(magnetLink, 30);// , TEMP_FOLDER.toFile());
                    if (magnetData != null) {

                        File magnetDataFile = new File(updatedEntryWrapper.getValue().magnetDataFile());
                        if (!magnetDataFile.exists()) {
                            LOG.error("Magnet data file does not exist for entry: {}", updatedEntryWrapper.getValue());
                            updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                            try {
                                new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                            } catch (Exception e) {
                                LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                            }
                            return updatedEntryWrapper.getValue();
                        }
                        try {
                            Files.write(magnetDataFile.toPath(), magnetData, StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING);
                        } catch (Exception e) {
                            LOG.error("Failed to write magnet data to file: {}", magnetDataFile.getAbsolutePath(), e);
                        }
                        break;
                    }
                }
            }
            if (magnetData == null) {
                LOG.error("Failed to fetch magnet data after 3 attempts");
                updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                try {
                    new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                } catch (Exception e) {
                    LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                }
                return updatedEntryWrapper.getValue();
            }

            AddTorrentParams addTorrentParams = null;
            if (updatedEntryWrapper.getValue().status() == ImportMediaStatus.PENDING
                    || updatedEntryWrapper.getValue().status() == ImportMediaStatus.QUEUED) {
                addTorrentParams = AddTorrentParams.parseMagnetUri(magnetLink);
                TorrentInfo info = new TorrentInfo(magnetData);
                addTorrentParams.torrentInfo(info);

            } else if (updatedEntryWrapper.getValue().status() == ImportMediaStatus.IN_PROGRESS) {
                // If the entry is already in progress, we need to load the resume data from
                // the resume file.
                File resumeFile = new File(updatedEntryWrapper.getValue().resumeFile());
                if (!resumeFile.exists()) {
                    LOG.error("Resume file does not exist for entry: {}", updatedEntryWrapper.getValue());
                    updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                    } catch (Exception e) {
                        LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                    }
                    return updatedEntryWrapper.getValue();
                }

                try {
                    byte[] resumeData = Files.readAllBytes(resumeFile.toPath());
                    error_code ec = new error_code();
                    add_torrent_params params_swig = add_torrent_params
                            .read_resume_data(Vectors.bytes2byte_vector(resumeData), ec);
                    ErrorCode error = new ErrorCode(ec);
                    if (error.value() != 0) {
                        LOG.error("Failed to read resume data from file: {}. Error: {}", resumeFile.getAbsolutePath(),
                                error.message());
                        updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                        try {
                            new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                        } catch (Exception ex) {
                            LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", ex);
                        }
                        return updatedEntryWrapper.getValue();
                    }

                    addTorrentParams = new AddTorrentParams(params_swig);
                    addTorrentParams.torrentInfo(new TorrentInfo(magnetData));
                } catch (Exception ex) {
                    LOG.error("Failed to read resume data from file: {}", resumeFile.getAbsolutePath(), ex);
                    updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                    } catch (Exception ex2) {
                        LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", ex2);
                    }
                    return updatedEntryWrapper.getValue();
                }
            }
            if (addTorrentParams == null) {
                LOG.error("Failed to create add_torrent_params for entry: {}", updatedEntryWrapper.getValue());
                updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED));
                try {
                    new ImportMediaEntry.Dao().update(updatedEntryWrapper.getValue());
                } catch (Exception e) {
                    LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                }
                return updatedEntryWrapper.getValue();
            }

            // Add an alert listener to handle torrent events after all updates to the entry
            // have been made to ensure that the entry is in a consistent state before
            // processing any alerts.

            TorrentInfo info = addTorrentParams.torrentInfo();
            FileStorage storage = info.files();

            List<FilePathRecord> filePathRecords = new ArrayList<>();

            boolean hasTopPriorityFile = false;
            Priority[] filePriorities = Priority.array(Priority.IGNORE, info.numFiles());
            for (int i = 0; i < filePriorities.length; i++) {
                String filePath = storage.filePath(i);
                Path path = Path.of(filePath);

                if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                        filePath.endsWith(".avi")) {
                    filePriorities[i] = hasTopPriorityFile ? Priority.NORMAL : Priority.SEVEN;
                    hasTopPriorityFile = true;

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

            if (entry.status() == ImportMediaStatus.PENDING) {
                CatalogUpdateProducer.postMessage(new CatalogUpdate(CatalogUpdateType.CREATED,
                        entry.category(), filePathRecords));
            }

            addTorrentParams.savePath(destinationFolder.toAbsolutePath().toString());

            addTorrentParams.flags(TorrentFlags.SEQUENTIAL_DOWNLOAD);

            final Wrapper<Integer> lastLoggedProgressWrapper = new Wrapper<>(-1);

            CompletableFuture<TorrentHandle> futureHandler = s.addTorrent(addTorrentParams,
                    TorrentAlertHandler.of(AddTorrentAlert.class, a -> {
                        LOG.info("Torrent added: {}", a.handle().name());
                        updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.IN_PROGRESS)
                                .withTorrentHash(a.handle().infoHash().toHex()));
                        var updatedEntry = updatedEntryWrapper.getValue();
                        try {
                            new ImportMediaEntry.Dao().update(updatedEntry);
                        } catch (SQLException e) {
                            LOG.error("Failed to update entry status to IN_PROGRESS", e);
                        }
                    }),
                    TorrentAlertHandler.of(SaveResumeDataAlert.class, a -> {
                        byte[] resumeData = Vectors
                                .byte_vector2bytes(add_torrent_params.write_resume_data_buf(a.params().swig()));
                        File resumeFile = new File(updatedEntryWrapper.getValue().resumeFile());
                        try {
                            Files.write(resumeFile.toPath(), resumeData, StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING);
                        } catch (Exception e) {
                            LOG.error("Failed to write resume data to file: {}", resumeFile.getAbsolutePath(), e);
                        }
                    }),
                    TorrentAlertHandler.of(FileCompletedAlert.class, a -> {
                        int index = a.index();
                        TorrentHandle alertHandle = a.handle();
                        TorrentInfo alertInfo = a.handle().torrentFile();
                        FileStorage alertStorage = a.handle().torrentFile().files();
                        Priority[] priorities = Priority.array(Priority.IGNORE, alertStorage.numFiles());

                        for (int i = 0; i < priorities.length; i++) {
                            String filePath = alertStorage.filePath(i);
                            if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                                    filePath.endsWith(".avi")) {
                                if (index + 1 != i) {
                                    priorities[i] = Priority.NORMAL;
                                } else {
                                    priorities[i] = Priority.SEVEN;
                                }
                            }
                        }
                        updatePriorities(alertHandle, priorities);
                        // setPieceDeadlinesForFile(alertHandle, alertInfo, alertStorage, index);
                    }),
                    TorrentAlertHandler.of(BlockFinishedAlert.class, a -> {
                        int p = (int) (a.handle().status().progress() * 100);
                        if (p % 10 == 0 && p != lastLoggedProgressWrapper.getValue()) {
                            LOG.info("Progress: {}% for torrent name: {}", p, a.handle().name());
                            lastLoggedProgressWrapper.setValue(p);
                        }
                    }));

            futureHandler.thenAccept((TorrentHandle handle) -> {
                LOG.info("Torrent download finished for: {}", handle.name());
                updatedEntryWrapper.apply(e -> e.withStatus(ImportMediaStatus.TORRENT_DOWNLOADED));
                var updatedEntry = updatedEntryWrapper.getValue();
                try {
                    new ImportMediaEntry.Dao().update(updatedEntry);
                } catch (SQLException e) {
                    LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
                }
            }).join();

        } catch (TorrentException e1) {
            LOG.error("Failed to add torrent", e1);
        } finally {
            // signal.countDown();
            // if (saveResumeDataTask != null) {
            // saveResumeDataTask.cancel(true);
            // }
            // if (listener != null) {
            // TorrentSession.getInstance().removeListenerForHandle(listener.getTorrentHandle());
            // }
        }
        return updatedEntryWrapper.getValue();
    }

    void setPieceDeadlinesForFile(TorrentHandle handle, TorrentInfo info, FileStorage storage, int fileIndex) {
        int fileSize = (int) storage.fileSize(fileIndex);
        if (fileSize == 0) {
            LOG.info("File size is 0 for file: {} in torrent: {}", storage.filePath(fileIndex), info.name());
            return;
        }

        PeerRequest pr = info.mapFile(fileIndex, 0, fileSize);
        int startIndex = pr.piece();

        int numPieces = pr.length();// + pieceLength - 1;
        int endIndex = startIndex + numPieces - 1;

        int deadline = 1;
        for (int j = startIndex; j <= endIndex; j++) {
            handle.setPieceDeadline(j, deadline++);
        }
    }

    private void updatePriorities(TorrentHandle handle, Priority[] priorities) {
        for (int i = 0; i < priorities.length; i++) {
            handle.filePriority(i, priorities[i]);
        }
    }
}
