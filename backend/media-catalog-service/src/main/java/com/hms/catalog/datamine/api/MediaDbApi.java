package com.hms.catalog.datamine.api;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;

public abstract class MediaDbApi {

    private final ThreadPoolTaskExecutor taskExecutor;

    protected MediaDbApi() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setThreadNamePrefix("MediaDbApi-");
        taskExecutor.setVirtualThreads(true);
        taskExecutor.initialize();
    }

    protected <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        return taskExecutor.submitCompletable(task);
    }

    public final CompletableFuture<Series> searchSeries(Series series) {
        return executeAsync(() -> searchSeriesImpl(series));
    }

    protected abstract Series searchSeriesImpl(Series series);

    public final CompletableFuture<Movie> searchMovie(Movie movie) {
        return executeAsync(() -> searchMovieImpl(movie));
    }

    protected abstract Movie searchMovieImpl(Movie movie);
}
