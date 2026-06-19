package com.hms.stream.importmedia;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;
import com.hms.stream.importmedia.pipeline.ImportMediaPipeline;

/**
 * Polls the database for pending media import requests and processes them.
 */
@Service
public class MediaImportTaskRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MediaImportTaskRunner.class);

    private final Map<String, Runnable> taskMap = new ConcurrentHashMap<>();

    ThreadPoolTaskScheduler scheduler;
    ThreadPoolTaskExecutor executor;

    public MediaImportTaskRunner() {
        executor = new ThreadPoolTaskExecutor();
        // executor.setVirtualThreads(true);
        executor.initialize();

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        scheduler.scheduleAtFixedRate(this, Duration.ofSeconds(1));
    }

    @Override
    public void run() {
        try {
            var dao = new ImportMediaEntry.Dao();
            dao.select(Map.of("status", ImportMediaStatus.PENDING.name()))
                    .stream()
                    .sorted((e1, e2) -> e1.createdAt().compareTo(e2.createdAt()))
                    .findFirst()
                    .ifPresent(this::addProcessingTask);
        } catch (SQLException e) {
            LOG.error("Error while processing media import tasks", e);
        }
    }

    private void addProcessingTask(ImportMediaEntry entry) {
        if (taskMap.containsKey(entry.id())) {
            LOG.info("Task for entry {} is already running. Skipping.", entry.id());
            return;
        }
        Runnable task = () -> {
            try {
                processImport(entry);
            } finally {
                taskMap.remove(entry.id());
            }
        };
        taskMap.put(entry.id(), task);
        executor.execute(task);
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
        var dao = new ImportMediaEntry.Dao();
        ImportMediaPipeline pipeline = ImportMediaPipeline.builder()
                .addHandler((e) -> {
                    ImportMediaEntry updatedEntry = e.withStatus(ImportMediaStatus.IN_PROGRESS);
                    dao.update(updatedEntry);
                    return updatedEntry;
                })
                .addHandler(new TorrentMagnetLink())
                .addHandler((e) -> {
                    ImportMediaEntry updatedEntry = e.withStatus(ImportMediaStatus.COMPLETED);
                    dao.update(updatedEntry);
                    return updatedEntry;
                })
                .onError((ent, ex) -> {
                    LOG.error("Error processing media import for entry: " + ent.id(), ex);
                    ImportMediaEntry updatedEntry = ent.withStatus(ImportMediaStatus.FAILED);
                    try {
                        dao.update(updatedEntry);
                    } catch (SQLException e1) {
                        LOG.error("Failed to update media import status to FAILED", e1);
                    }
                })
                .build();

        ImportMediaEntry updatedEntry = pipeline.handle(entry);
    }
}
