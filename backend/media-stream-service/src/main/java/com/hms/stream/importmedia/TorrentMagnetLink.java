package com.hms.stream.importmedia;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.frostwire.jlibtorrent.AddTorrentParams;
import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.ErrorCode;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.PeerRequest;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.SessionHandle;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentFlags;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Vectors;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.FileCompletedAlert;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.swig.add_torrent_params;
import com.frostwire.jlibtorrent.swig.error_code;
import com.hms.shared.media.MediaItem;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;
import com.hms.stream.importmedia.pipeline.ImportMediaHandler;
import com.hms.stream.messaging.CatalogUpdateProducer;

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

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {

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

        // libtorrent4j does not work in docker container due to missing dependencies.
        // Will need to switch to a different torrent library or implement torrent
        // downloading in a separate service that runs on the host machine and can
        // access the torrent download directory directly.
        final CountDownLatch signal = new CountDownLatch(1);

        ImportMediaAlertListener listener = null;
        ScheduledFuture<?> saveResumeDataTask = null;
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            SessionManager s = torrentSession.getSessionManager();
            SessionHandle sh = new SessionHandle(s.swig());

            entry = ensureResumeFileExists(entry, destinationFolder);
            entry = ensureMagnetDataFileExists(entry, destinationFolder);

            byte[] magnetData = null;
            if (entry.magnetDataFile() != null
                    && entry.status() != ImportMediaStatus.QUEUED && entry.status() != ImportMediaStatus.PENDING) {
                File magnetDataFile = new File(entry.magnetDataFile());
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

                        File magnetDataFile = new File(entry.magnetDataFile());
                        if (!magnetDataFile.exists()) {
                            LOG.error("Magnet data file does not exist for entry: {}", entry);
                            entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                            try {
                                new ImportMediaEntry.Dao().update(entry);
                            } catch (Exception e) {
                                LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                            }
                            return entry;
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
                entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                try {
                    new ImportMediaEntry.Dao().update(entry);
                } catch (Exception e) {
                    LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                }
                return entry;
            }

            AddTorrentParams addTorrentParams = null;
            if (entry.status() == ImportMediaStatus.PENDING || entry.status() == ImportMediaStatus.QUEUED) {
                addTorrentParams = AddTorrentParams.parseMagnetUri(magnetLink);
                TorrentInfo info = new TorrentInfo(magnetData);
                addTorrentParams.torrentInfo(info);

            } else if (entry.status() == ImportMediaStatus.IN_PROGRESS) {
                // If the entry is already in progress, we need to load the resume data from
                // the resume file.
                File resumeFile = new File(entry.resumeFile());
                if (!resumeFile.exists()) {
                    LOG.error("Resume file does not exist for entry: {}", entry);
                    entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                    try {
                        new ImportMediaEntry.Dao().update(entry);
                    } catch (Exception e) {
                        LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                    }
                    return entry;
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
                        entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                        try {
                            new ImportMediaEntry.Dao().update(entry);
                        } catch (Exception ex) {
                            LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", ex);
                        }
                        return entry;
                    }

                    addTorrentParams = new AddTorrentParams(params_swig);
                    addTorrentParams.torrentInfo(new TorrentInfo(magnetData));
                } catch (Exception e) {
                    LOG.error("Failed to read resume data from file: {}", resumeFile.getAbsolutePath(), e);
                    entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                    try {
                        new ImportMediaEntry.Dao().update(entry);
                    } catch (Exception ex) {
                        LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", ex);
                    }
                    return entry;
                }
            }
            if (addTorrentParams == null) {
                LOG.error("Failed to create add_torrent_params for entry: {}", entry);
                entry = entry.withStatus(ImportMediaStatus.MAGNET_FETCH_FAILED);
                try {
                    new ImportMediaEntry.Dao().update(entry);
                } catch (Exception e) {
                    LOG.error("Failed to update entry status to MAGNET_FETCH_FAILED", e);
                }
                return entry;
            }

            // Add an alert listener to handle torrent events after all updates to the entry
            // have been made to ensure that the entry is in a consistent state before
            // processing any alerts.
            listener = new ImportMediaAlertListener(entry, signal);
            s.addListener(listener);

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

            ErrorCode error = new ErrorCode(new error_code());

            TorrentHandle handle = sh.addTorrent(addTorrentParams, error);

            for (int i = 0; i < filePriorities.length; i++) {
                int fileSize = (int) storage.fileSize(i);
                if (fileSize == 0 || filePriorities[i] == Priority.IGNORE) {
                    LOG.info("File size is 0 for file: {} in torrent: {}", storage.filePath(i), info.name());
                    continue;
                }

                PeerRequest pr = info.mapFile(i, 0, fileSize);
                int startIndex = pr.piece();

                int numPieces = pr.length();// + pieceLength - 1;
                int endIndex = startIndex + numPieces - 1;

                int deadline = 1;
                for (int j = startIndex; j <= endIndex; j++) {
                    handle.setPieceDeadline(j, deadline++);
                }
                break; // Only set deadlines for the first valid file
            }

            saveResumeDataTask = scheduler.scheduleAtFixedRate(() -> {
                handle.saveResumeData();
            }, Duration.ofSeconds(1));

            try {
                signal.await();
            } catch (InterruptedException e) {
                LOG.error("Torrent download interrupted", e);
                Thread.currentThread().interrupt();
            }

        } finally {
            signal.countDown();
            if (saveResumeDataTask != null) {
                saveResumeDataTask.cancel(true);
            }
            if (listener != null) {
                TorrentSession.getInstance().getSessionManager().removeListener(listener);
            }
        }
        return listener != null ? listener.getUpdatedEntry() : entry;
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

        private void updatePriorities(TorrentHandle handle, Priority[] priorities) {
            for (int i = 0; i < priorities.length; i++) {
                handle.filePriority(i, priorities[i]);
            }
        }

        @Override
        public void alert(Alert<?> alert) {
            switch (alert) {
                case AddTorrentAlert a -> {
                    LOG.info("Torrent added: {}", a.handle().name());
                    updatedEntry = updatedEntry.withStatus(ImportMediaStatus.IN_PROGRESS);
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntry);
                    } catch (SQLException e) {
                        LOG.error("Failed to update entry status to IN_PROGRESS", e);
                    }
                    break;
                }
                case TorrentFinishedAlert finishedAlert -> {
                    LOG.info("Torrent download finished for: {}", finishedAlert.handle().name());
                    updatedEntry = updatedEntry.withStatus(ImportMediaStatus.TORRENT_DOWNLOADED);
                    try {
                        new ImportMediaEntry.Dao().update(updatedEntry);
                    } catch (SQLException e) {
                        LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
                    }
                    signal.countDown();
                    break;
                }
                case SaveResumeDataAlert a -> {
                    byte[] resumeData = Vectors
                            .byte_vector2bytes(add_torrent_params.write_resume_data_buf(a.params().swig()));
                    File resumeFile = new File(entry.resumeFile());
                    try {
                        Files.write(resumeFile.toPath(), resumeData, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (Exception e) {
                        LOG.error("Failed to write resume data to file: {}", resumeFile.getAbsolutePath(), e);
                    }
                    break;
                }
                case FileCompletedAlert a -> {
                    int index = a.index();
                    TorrentHandle handle = a.handle();
                    TorrentInfo info = a.handle().torrentFile();
                    FileStorage storage = a.handle().torrentFile().files();
                    Priority[] priorities = Priority.array(Priority.IGNORE, storage.numFiles());

                    for (int i = 0; i < priorities.length; i++) {
                        String filePath = storage.filePath(i);
                        if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                                filePath.endsWith(".avi")) {
                            if (index + 1 != i) {
                                priorities[i] = Priority.NORMAL;
                            } else {
                                priorities[i] = Priority.SEVEN;
                            }
                        }
                    }
                    updatePriorities(handle, priorities);
                    setPieceDeadlinesForFile(handle, info, storage, index);
                    break;
                }
                case BlockFinishedAlert a -> {
                    int p = (int) (a.handle().status().progress() * 100);
                    if (p % 10 == 0 && p != lastLoggedProgress) {
                        LOG.info("Progress: {}% for torrent name: {}", p, a.handle().name());
                        lastLoggedProgress = p;
                    }
                    break;
                }
                default -> {
                }
            }            
        }
    }
}
