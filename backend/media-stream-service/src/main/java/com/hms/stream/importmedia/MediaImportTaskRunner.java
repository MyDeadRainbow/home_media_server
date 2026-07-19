package com.hms.stream.importmedia;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Flow.Publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.hms.shared.util.PollingService;
import com.hms.shared.util.TaskService;
import com.hms.stream.importmedia.pipeline.ImportMediaPipeline;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Polls the database for pending media import requests and processes them.
 */
@Service
public class MediaImportTaskRunner extends TaskService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaImportTaskRunner.class);

    public MediaImportTaskRunner() {
        super();
        init();
    }
    
    @Override
    public void configure(ThreadPoolTaskExecutor executor, ThreadPoolTaskScheduler scheduler) {
        // TODO Auto-generated method stub
        
    }

    private void init() {
        try {
            var dao = new ImportMediaEntry.Dao();
            dao.select(Map.of("status", ImportMediaStatus.IN_PROGRESS.name()))
                    .stream()
                    .sorted((e1, e2) -> e1.createdAt().compareTo(e2.createdAt()))
                    .forEach(this::addProcessingTask);

            Disposable disposable = dao.listen().subscribe(entry -> {
                if (hasTask(entry.id())) {
                    LOG.info("Media import task already in progress for entry: " + entry.id());
                    return;
                }
                if (entry.status() == ImportMediaStatus.PENDING || entry.status() == ImportMediaStatus.RESUME) {
                    LOG.info("New media import task detected for entry: " + entry.id());
                    addProcessingTask(entry);
                }
            }, e -> LOG.error("Error while processing media import tasks", e));
        } catch (SQLException e) {
            LOG.error("Error while processing media import tasks", e);
        }
    }

    // @Override
    // public Duration pollingInterval() {
    //     return Duration.ofSeconds(1);
    // }

    // @Override
    // public void poll() {
    //     // try {
    //     // var dao = new ImportMediaEntry.Dao();
    //     // dao.select(Map.of("status", ImportMediaStatus.PENDING.name()))
    //     // .stream()
    //     // .filter(e -> !hasTask(e.id()))
    //     // .sorted((e1, e2) -> e1.createdAt().compareTo(e2.createdAt()))
    //     // .forEach(this::addProcessingTask);
    //     // dao.select(Map.of("status", ImportMediaStatus.RESUME.name()))
    //     // .stream()
    //     // .filter(e -> !hasTask(e.id()))
    //     // .sorted((e1, e2) -> e1.createdAt().compareTo(e2.createdAt()))
    //     // .forEach(this::addProcessingTask);
    //     // } catch (SQLException e) {
    //     // LOG.error("Error while processing media import tasks", e);
    //     // }
    // }

    private void addProcessingTask(ImportMediaEntry entry) {
        Runnable task = () -> {
            processImport(entry);
        };

        submit(entry.id(), task);
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
                .addHandler(new TorrentMagnetLink())
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

        pipeline.handle(entry);
    }
}
