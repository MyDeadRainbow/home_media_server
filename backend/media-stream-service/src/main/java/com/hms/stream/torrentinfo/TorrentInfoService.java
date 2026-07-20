package com.hms.stream.torrentinfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentStatus;
import com.hms.shared.media.FileName;
import com.hms.shared.media.MediaItem;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;
import com.hms.shared.util.Wrapper;
import com.hms.stream.importmedia.ImportMediaEntry;
import com.hms.stream.importmedia.ImportMediaEntryMediaItem;
import com.hms.stream.importmedia.ImportMediaStatus;
import com.hms.stream.messaging.CatalogUpdateProducer;
import com.hms.stream.torrentsession.TorrentSession;

import io.reactivex.rxjava3.core.Observable;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

@Service
public class TorrentInfoService {

    private final Logger LOG = LoggerFactory.getLogger(TorrentInfoService.class);
    private final CatalogUpdateProducer catalogUpdateProducer;

    public TorrentInfoService(CatalogUpdateProducer catalogUpdateProducer) {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("SseEmitterTaskScheduler-");
        taskScheduler.setVirtualThreads(true);
        taskScheduler.initialize();
        this.catalogUpdateProducer = catalogUpdateProducer;
    }

    public List<TorrentInfoResponse> getTorrentInfo() {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            List<ImportMediaEntry> entries = new ImportMediaEntry.Dao().select(Map.of());
            List<TorrentInfoResponse> responses = entries.stream().map(entry -> {
                TorrentHandle handle = torrentSession.getTorrentHandle(entry.torrentHash());
                if (handle == null || !handle.isValid()) {
                    return new TorrentInfoResponse(
                            entry.title(),
                            entry.torrentHash(),
                            -1,
                            0,
                            0,
                            0,
                            0,
                            0,
                            entry.status());
                }
                TorrentStatus status = handle.status();
                return new TorrentInfoResponse(
                        entry.title(),
                        entry.torrentHash(),
                        status.queuePosition(),
                        status.totalWanted(),
                        status.totalDone(),
                        status.uploadRate(),
                        status.downloadRate(),
                        status.numPeers(),
                        entry.status());
            }).sorted((ti1, ti2) -> Integer.compare(ti1.queuePosition(), ti2.queuePosition())).toList();

            return responses;
        } catch (SQLException e) {
            LOG.error("Error while fetching torrent info", e);
        }
        return List.of();
    }

    public boolean pauseTorrent(String infoHash) {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            boolean result = torrentSession.pauseTorrent(infoHash);
            try {
                ImportMediaEntry entry = new ImportMediaEntry.Dao().select(Map.of("torrentHash", infoHash)).stream()
                        .findFirst().orElse(null);
                if (entry != null) {
                    new ImportMediaEntry.Dao().update(entry.withStatus(ImportMediaStatus.PAUSED));
                }
            } catch (SQLException e) {
                LOG.error("Failed to update media import status to TORRENT_PAUSED", e);
            }
            return result;
        }
    }

    public boolean resumeTorrent(String infoHash) {
        ImportMediaEntry entry = null;
        boolean result = false;
        try {
            entry = new ImportMediaEntry.Dao().select(Map.of("torrentHash", infoHash)).stream()
                    .findFirst().orElse(null);
            if (entry != null) {
                try (TorrentSession torrentSession = TorrentSession.getInstance()) {
                    TorrentHandle handle = torrentSession.getTorrentHandle(infoHash);
                    if (handle != null && handle.isValid()) {
                        result = torrentSession.resumeTorrent(infoHash);        
                        if (result) {
                            new ImportMediaEntry.Dao().update(entry.withStatus(ImportMediaStatus.IN_PROGRESS));
                        }                                        
                    } else {
                        new ImportMediaEntry.Dao().update(entry.withStatus(ImportMediaStatus.RESUME));
                        result = true;
                    }

                    // return result;
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to update media import status to RESUME", e);
        }
        return result;
    }

    public boolean deleteTorrent(String infoHash) {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            boolean result = torrentSession.deleteTorrent(infoHash);
            try {
                ImportMediaEntry entry = new ImportMediaEntry.Dao().select(Map.of("torrentHash", infoHash)).stream()
                        .findFirst().orElse(null);
                if (entry != null) {
                    List<ImportMediaEntryMediaItem> items = entry.items();
                    for (ImportMediaEntryMediaItem item : items) {
                        MediaItem mediaItem = new MediaItem.Dao().get(item.mediaId());
                        if (mediaItem != null) {
                            new MediaItem.Dao().delete(mediaItem);

                            catalogUpdateProducer.sendMessage(new CatalogUpdate(CatalogUpdateType.DELETED,
                                    entry.category(), List.of(new FilePathRecord(mediaItem.mediaId(), null))));
                        }
                        new ImportMediaEntryMediaItem.Dao().delete(item);
                    }

                    String torrentFolderPathStr = entry.torrentFolderPath();
                    if (torrentFolderPathStr != null && !torrentFolderPathStr.isEmpty()) {
                        Path torrentFolderPath = Path.of(entry.torrentFolderPath());
                        if (Files.exists(torrentFolderPath)) {
                            try {
                                Files.walk(torrentFolderPath)
                                        .sorted(Comparator.reverseOrder())
                                        .map(Path::toFile)
                                        .forEach(File::delete);
                            } catch (IOException e) {
                                LOG.error("Failed to delete torrent folder: {}", torrentFolderPathStr, e);
                            }
                        }
                    }
                    String resumeFilePathStr = entry.resumeFile();
                    if (resumeFilePathStr != null && !resumeFilePathStr.isEmpty()) {
                        Path resumeFilePath = Path.of(entry.resumeFile());
                        if (Files.exists(resumeFilePath)) {
                            try {
                                Files.delete(resumeFilePath);
                            } catch (IOException e) {
                                LOG.error("Failed to delete resume file: {}", resumeFilePathStr, e);
                            }
                        }
                    }
                    String magnetDataFilePathStr = entry.magnetDataFile();
                    if (magnetDataFilePathStr != null && !magnetDataFilePathStr.isEmpty()) {
                        Path magnetDataFilePath = Path.of(entry.magnetDataFile());
                        if (Files.exists(magnetDataFilePath)) {
                            try {
                                Files.delete(magnetDataFilePath);
                            } catch (IOException e) {
                                LOG.error("Failed to delete magnet data file: {}", magnetDataFilePathStr, e);
                            }
                        }
                    }

                    new ImportMediaEntry.Dao().update(entry.withStatus(ImportMediaStatus.DELETED)
                            .withMagnetDataFile(null)
                            .withResumeFile(null)
                            .withItems(List.of())
                            .withTorrentFolderPath(null)
                            .withTorrentHash(null));
                }
            } catch (SQLException e) {
                LOG.error("Failed to update media import status to DELETED", e);
            }
            return result;
        }
    }

    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> infoStreamTasks = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public ResponseEntity<SseEmitter> getTorrentInfoStream(String infoHash) {
        Wrapper<ScheduledFuture<?>> future = new Wrapper<>(null);
        if (infoHash == null || infoHash.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        SseEmitter emitter = new SseEmitter();

        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            if (torrentSession.getTorrentHandle(infoHash) == null) {
                return ResponseEntity.notFound().build();
            }
            future.set(taskScheduler.scheduleAtFixedRate(() -> {
                TorrentHandle handle = torrentSession.getTorrentHandle(infoHash);
                if (handle == null || !handle.isValid()) {
                    return;
                }
                TorrentStatus status = handle.status();
                if (status != null) {
                    ImportMediaEntry entry = null;
                    try {
                        entry = new ImportMediaEntry.Dao().select(Map.of("torrentHash", infoHash)).stream()
                                .findFirst().orElse(null);
                    } catch (SQLException e) {
                        LOG.error("Error while fetching media import entry for infoHash: {}", infoHash, e);
                    }
                    ImportMediaStatus importMediaStatus = entry != null ? entry.status() : null;
                    TorrentInfoUpdate update = new TorrentInfoUpdate(
                            infoHash,
                            status.totalDone(),
                            status.uploadRate(),
                            status.downloadRate(),
                            status.numPeers(),
                            importMediaStatus);
                    try {
                        emitter.send(update);
                    } catch (IOException e) {
                        LOG.error("Error sending torrent info update: {}", e.getMessage(), e);
                        future.apply(f -> {
                            f.cancel(true);
                            return f;
                        });
                    }
                }
            }, Duration.ofSeconds(1)));

            infoStreamTasks.put(infoHash, future.get());

            emitters.put(infoHash, emitter);
            emitter.onCompletion(() -> {
                emitters.remove(infoHash);
                cancelInfoStream(infoHash);
            });
            emitter.onTimeout(() -> {
                emitters.remove(infoHash);
                cancelInfoStream(infoHash);
            });
            emitter.onError((e) -> {
                emitters.remove(infoHash);
                cancelInfoStream(infoHash);
            });
        }
        return new ResponseEntity<>(emitter, HttpStatus.OK);
    }

    public void cancelInfoStream(String infoHash) {
        ScheduledFuture<?> scheduledTask = infoStreamTasks.get(infoHash);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            infoStreamTasks.remove(infoHash);
        }
        SseEmitter emitter = emitters.get(infoHash);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(infoHash);
        }
    }

    public void cancelEmitter(String infoHash) {
        SseEmitter emitter = emitters.get(infoHash);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(infoHash);
        }
    }

    public ResponseEntity<SseEmitter> getMediaItemInfoStream(String mediaItemId) {
        SseEmitter emitter = new SseEmitter();
        Wrapper<ScheduledFuture<?>> future = new Wrapper<>(null);

        try (TorrentSession torrentSession = TorrentSession.getInstance()) {

            Wrapper<Long> lastBytesDownloaded = new Wrapper<>(-1L);
            future.set(taskScheduler.scheduleAtFixedRate(() -> {
                ImportMediaEntry entry = null;
                try {
                    ImportMediaEntryMediaItem mediaItemRecord = new ImportMediaEntryMediaItem.Dao()
                            .select(Map.of("mediaId", mediaItemId)).stream().findFirst().orElse(null);
                    if (mediaItemRecord != null) {
                        entry = new ImportMediaEntry.Dao().select(Map.of("id", mediaItemRecord.importMediaEntryId()))
                                .stream()
                                .findFirst().orElse(null);
                    }
                } catch (SQLException e) {
                    LOG.error("Error while fetching media import entry for mediaItemId: {}", mediaItemId, e);
                    emitter.complete();
                }

                FileName fileNameRecord = null;
                try {
                    fileNameRecord = new FileName.Dao().select(Map.of("mediaId", mediaItemId)).stream()
                            .findFirst().orElse(null);
                } catch (SQLException e) {
                    LOG.error("Error while fetching file name record for mediaItemId: {}", mediaItemId, e);
                    emitter.complete();
                }

                if (entry != null && fileNameRecord != null) {
                    TorrentHandle handle = torrentSession.getTorrentHandle(entry.torrentHash());
                    if (handle == null || !handle.isValid()) {
                        emitter.complete();
                        return;
                    }
                    TorrentStatus status = handle.status();

                    long[] fileProgress = handle.fileProgress();
                    FileStorage fileStorage = handle.torrentFile().files();
                    for (int i = 0; i < fileStorage.numFiles(); i++) {
                        String fileName = fileStorage.fileName(i);
                        if (fileName.equals(fileNameRecord.fileName())) {
                            long fileSize = fileStorage.fileSize(i);
                            long bytesDownloaded = fileProgress[i];

                            MediaItem mediaItem = fileNameRecord.mediaItem();

                            Path videoPath = Path.of(mediaItem.filePath()).normalize();
                            if (!(Files.exists(videoPath) && Files.isRegularFile(videoPath))) {
                                throw new IllegalArgumentException(
                                        "Stored file not found for storageId: " + mediaItem.mediaId());
                            }

                            long requiredByteDownloadRate = -1;

                            try {

                                MultimediaObject instance = new MultimediaObject(videoPath.toFile());
                                MultimediaInfo info = instance.getInfo();

                                // Duration returns in milliseconds
                                long durationMillis = info.getDuration();
                                long durationSeconds = durationMillis / 1000;
                                requiredByteDownloadRate = fileSize / durationSeconds;
                            } catch (Exception e) {
                                LOG.error("Error while parsing video file for mediaItemId: {}", mediaItem.mediaId(), e);
                            }
                            // try (InputStream stream = Files.newInputStream(videoPath)) {
                            // AutoDetectParser parser = new AutoDetectParser();
                            // Metadata metadata = new Metadata();

                            // // Parse the video file to populate metadata
                            // parser.parse(stream, new BodyContentHandler(), metadata, new ParseContext());

                            // // Retrieve the duration property
                            // String duration = metadata.get(XMPDM.DURATION);
                            // int durationInSeconds = -1;
                            // if (duration != null) {
                            // try {
                            // durationInSeconds = (int) Double.parseDouble(duration);
                            // requiredByteDownloadRate = fileSize / durationInSeconds;
                            // } catch (NumberFormatException e) {
                            // LOG.error("Error parsing duration: {}", duration, e);
                            // }
                            // }
                            // } catch (Exception e) {
                            // LOG.error("Error while parsing video file for mediaItemId: {}", mediaItemId,
                            // e);
                            // }

                            MediaItemInfoUpdate update = new MediaItemInfoUpdate(
                                    mediaItemId,
                                    fileSize,
                                    bytesDownloaded,
                                    // status.uploadRate(),
                                    bytesDownloaded - lastBytesDownloaded.get(),
                                    requiredByteDownloadRate,
                                    // status.numPeers(),
                                    entry.status());
                            lastBytesDownloaded.set(bytesDownloaded);
                            try {
                                emitter.send(update);
                            } catch (IOException e) {
                                LOG.error("Error sending media item info update: {}", e.getMessage(), e);
                                future.apply(f -> {
                                    f.cancel(true);
                                    return f;
                                });
                            }
                            break;
                        }
                    }
                }
            }, Duration.ofSeconds(1)));

            infoStreamTasks.put(mediaItemId, future.get());

            emitters.put(mediaItemId, emitter);
            emitter.onCompletion(() -> {
                emitters.remove(mediaItemId);
                cancelInfoStream(mediaItemId);
            });
            emitter.onTimeout(() -> {
                emitters.remove(mediaItemId);
                cancelInfoStream(mediaItemId);
            });
            emitter.onError((e) -> {
                emitters.remove(mediaItemId);
                cancelInfoStream(mediaItemId);
            });
        }
        return new ResponseEntity<>(emitter, HttpStatus.OK);
    }
}
