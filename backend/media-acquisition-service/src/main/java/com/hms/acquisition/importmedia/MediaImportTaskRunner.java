package com.hms.acquisition.importmedia;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.hms.acquisition.importmedia.magnetfinder.OneThreeThreeSevenXMagnetFinder;
import com.hms.acquisition.importmedia.magnetfinder.PirateBayMagnetFinder;
import com.hms.acquisition.importmedia.pipeline.ImportMediaPipeline;
import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;
import com.hms.shared.dao.SQLiteSerializable;

/**
 * Polls the database for pending media import requests and processes them.
 */
@Service
public class MediaImportTaskRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MediaImportTaskRunner.class);

    ThreadPoolTaskScheduler scheduler;
    ThreadPoolTaskExecutor executor;

    public MediaImportTaskRunner() {
        executor = new ThreadPoolTaskExecutor();        
        executor.setVirtualThreads(true);
        executor.initialize();

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        scheduler.scheduleAtFixedRate(this, Duration.ofSeconds(1));
    }

    @Override
    public void run() {
        try {
            SQLiteSerializable
                    .select(ImportMediaEntry.class,
                            Map.of())
                    .stream()
                    .filter(entry -> entry.status() == ImportMediaStatus.PENDING)
                    .forEach(this::addProcessingTask);
        } catch (DBFileNotFoundException | GetConnectionException | SQLException e) {
            LOG.error("Error while processing media import tasks", e);
        }
    }

    private void addProcessingTask(ImportMediaEntry entry) {
        executor.execute(() -> processImport(entry));
    }

    /**
     * Processes a single media import request. Updates the status of the request in
     * the database as it progresses.
     * Update status -> Find media -> magnet link -> download media -> send media to
     * stream service -> update status to completed
     * 
     * @param entry
     */
    private void processImport(ImportMediaEntry entry) {

        // Update status to IN_PROGRESS
        // ImportMediaEntry updatedEntry = new ImportMediaEntry(entry.id(),
        // entry.title(), ImportMediaStatus.IN_PROGRESS,
        // entry.magnetLink());
        // try {
            // updatedEntry.update();

            ImportMediaPipeline pipeline = ImportMediaPipeline.builder()
                    .addHandler((e) -> {
                        ImportMediaEntry updatedEntry = new ImportMediaEntry(e.id(), e.title(),
                                ImportMediaStatus.IN_PROGRESS,
                                e.magnetLink());
                        updatedEntry.update();
                        return updatedEntry;
                    })
                    .addHandler(new PirateBayMagnetFinder())
                    .addHandler(new OneThreeThreeSevenXMagnetFinder())
                    .addHandler(new TorrentMagnetLink())
                    .addHandler((e) -> {
                        ImportMediaEntry updatedEntry = new ImportMediaEntry(e.id(), e.title(), ImportMediaStatus.COMPLETED,
                                e.magnetLink());
                        updatedEntry.update();
                        return updatedEntry;
                    })
                    .onError((ent, ex) -> {
                        LOG.error("Error processing media import for entry: " + ent.id(), ex);
                        ImportMediaEntry updatedEntry = new ImportMediaEntry(ent.id(), ent.title(),
                                ImportMediaStatus.FAILED,
                                ent.magnetLink());
                        try {
                            updatedEntry.update();
                        } catch (DBFileNotFoundException | GetConnectionException | SQLException e1) {
                            LOG.error("Failed to update media import status to FAILED", e1);
                        }
                    })
                    .build();

            ImportMediaEntry updatedEntry = pipeline.handle(entry);

            // PirateBayMagnetFinder magnetFinder = new
            // PirateBayMagnetFinder(entry.title());
            // String magnetLink = magnetFinder.findBestMagnetLink();
            // if (magnetLink != null) {
            // updatedEntry = new ImportMediaEntry(entry.id(), entry.title(),
            // ImportMediaStatus.MAGNET_FOUND,
            // magnetLink);
            // updatedEntry.update();
            // } else {
            // LOG.warn("No magnet link found for media request: " + entry.id());
            // updatedEntry = new ImportMediaEntry(entry.id(), entry.title(),
            // ImportMediaStatus.MAGNET_NOT_FOUND,
            // null);
            // updatedEntry.update();
            // return;
            // }

            // updatedEntry = new ImportMediaEntry(entry.id(), entry.title(),
            // ImportMediaStatus.TORRENT_DOWNLOADING,
            // magnetLink);
            // updatedEntry.update();

            // TorrentMagnetLink torrentDownloader = new TorrentMagnetLink(updatedEntry);
            // torrentDownloader.run();

            // Update status to COMPLETED
            // updatedEntry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.COMPLETED,
            //         updatedEntry.magnetLink());
            // updatedEntry.update();
        // } catch (Exception e) {
        //     LOG.error("Error processing media import for entry: " + entry.id(), e);
        //     // Update status to FAILED in case of any errors
        //     updatedEntry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.FAILED,
        //             updatedEntry.magnetLink());
        //     try {
        //         updatedEntry.update();
        //     } catch (DBFileNotFoundException | GetConnectionException | SQLException e1) {
        //         LOG.error("Failed to update media import status to FAILED", e1);
        //     }
        // }
    }
}
