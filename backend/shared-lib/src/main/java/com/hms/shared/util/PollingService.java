package com.hms.shared.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public abstract class PollingService extends TaskService implements Runnable {

    public PollingService() {
        super();
        scheduleAtFixedRate("polling", this, pollingInterval());
    }

    @Override
    public void configure(ThreadPoolTaskExecutor executor, ThreadPoolTaskScheduler scheduler) {
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(100);
        executor.setMaxPoolSize(10);

        scheduler.setPoolSize(1);
    }

    @Override
    public final void run() {
        poll();
    }

    public abstract Duration pollingInterval();

    public abstract void poll();

    // // public Future<?> submit(String key, Runnable task) throws TaskAlreadyRunningException {
    // //     if (futureMap.containsKey(key)) {
    // //         throw new TaskAlreadyRunningException("Task with key " + key + " is already running.");
    // //     }
    // //     return futureMap.put(key, executor.submitCompletable(task).whenComplete((_, _) -> futureMap.remove(key)));
    // // }

    // // @SuppressWarnings("unchecked")
    // // public <T> Future<T> submit(String key, Callable<T> task) throws TaskAlreadyRunningException {
    // //     if (futureMap.containsKey(key)) {
    // //         throw new TaskAlreadyRunningException("Task with key " + key + " is already running.");
    // //     }
    // //     futureMap.put(key, executor.submitCompletable(task).whenComplete((_, _) -> futureMap.remove(key)));
    // //     return (Future<T>) futureMap.get(key);
    // // }

    // public CompletableFuture<?> submit(String key, Runnable task) {
    //     if (completableMap.containsKey(key)) {
    //         return completableMap.get(key);
    //     }
    //     return completableMap.put(key, executor.submitCompletable(task).whenComplete((_, _) -> completableMap.remove(key)));
    // }

    // // @SuppressWarnings("unchecked")
    // public <T> CompletableFuture<T> submit(String key, Callable<T> task) {
    //     if (completableMap.containsKey(key)) {
    //         try {
    //             return (CompletableFuture<T>) completableMap.get(key);
    //         } catch (ClassCastException e) {
    //             throw new IllegalStateException("Existing future is not of the expected type.", e);
    //         }
    //     }
    //     completableMap.put(key, executor.submitCompletable(task).whenComplete((_, _) -> completableMap.remove(key)));
    //     return (CompletableFuture<T>) completableMap.get(key);
    // }

    // public boolean hasTask(String key) {
    //     return completableMap.containsKey(key);
    // }

    // public static class TaskAlreadyRunningException extends Exception {
    //     public TaskAlreadyRunningException(String message) {
    //         super(message);
    //     }
    // }
}
