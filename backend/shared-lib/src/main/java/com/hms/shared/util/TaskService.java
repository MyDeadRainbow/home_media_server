package com.hms.shared.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public abstract class TaskService {
    protected final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();;
    protected final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();;

    protected final Map<String, ScheduledFuture<?>> scheduleMap = new ConcurrentHashMap<>();
    protected final Map<String, CompletableFuture<?>> completableMap = new ConcurrentHashMap<>();

    public TaskService() {
        configure(executor, scheduler);
        executor.initialize();
        scheduler.initialize();
    }

    public abstract void configure(ThreadPoolTaskExecutor executor, ThreadPoolTaskScheduler scheduler);

    public ScheduledFuture<?> scheduleAtFixedRate(String key, Runnable task, Duration period) {
        if (scheduleMap.containsKey(key)) {
            return scheduleMap.get(key);
        }
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, period);
        scheduleMap.put(key, future);
        return future;
    }

    public CompletableFuture<?> submit(String key, Runnable task) {
        if (completableMap.containsKey(key)) {
            return completableMap.get(key);
        }
        return completableMap.put(key,
                executor.submitCompletable(task).whenComplete((_, _) -> completableMap.remove(key)));
    }

    // @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submit(String key, Callable<T> task) {
        if (completableMap.containsKey(key)) {
            try {
                return (CompletableFuture<T>) completableMap.get(key);
            } catch (ClassCastException e) {
                throw new IllegalStateException("Existing future is not of the expected type.", e);
            }
        }
        completableMap.put(key, executor.submitCompletable(task).whenComplete((_, _) -> completableMap.remove(key)));
        return (CompletableFuture<T>) completableMap.get(key);
    }

    public boolean hasTask(String key) {
        return completableMap.containsKey(key);
    }

    public static class TaskAlreadyRunningException extends Exception {
        public TaskAlreadyRunningException(String message) {
            super(message);
        }
    }
}
