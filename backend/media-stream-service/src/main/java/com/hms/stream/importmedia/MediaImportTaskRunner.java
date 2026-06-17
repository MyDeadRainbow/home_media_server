package com.hms.stream.importmedia;

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

import com.hms.stream.importmedia.pipeline.ImportMediaPipeline;
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
        ImportMediaPipeline pipeline = ImportMediaPipeline.builder()
                .addHandler((e) -> {
                    ImportMediaEntry updatedEntry = e.withStatus(ImportMediaStatus.IN_PROGRESS);
                    updatedEntry.update();
                    return updatedEntry;
                })
                .addHandler(new TorrentMagnetLink())
                .addHandler((e) -> {
                    ImportMediaEntry updatedEntry = e.withStatus(ImportMediaStatus.COMPLETED);
                    updatedEntry.update();
                    return updatedEntry;
                })
                // .addHandler((e) -> {

                // })
                .onError((ent, ex) -> {
                    LOG.error("Error processing media import for entry: " + ent.id(), ex);
                    ImportMediaEntry updatedEntry = ent.withStatus(ImportMediaStatus.FAILED);
                    try {
                        updatedEntry.update();
                    } catch (DBFileNotFoundException | GetConnectionException | SQLException e1) {
                        LOG.error("Failed to update media import status to FAILED", e1);
                    }
                })
                .build();

        ImportMediaEntry updatedEntry = pipeline.handle(entry);
    }
}
