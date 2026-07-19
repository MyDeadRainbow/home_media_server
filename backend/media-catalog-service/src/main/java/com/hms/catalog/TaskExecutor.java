package com.hms.catalog;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.hms.shared.util.TaskService;

@Service
public class TaskExecutor extends TaskService {
    public TaskExecutor() {
        super();
    }

    @Override
    public void configure(ThreadPoolTaskExecutor executor, ThreadPoolTaskScheduler scheduler) {
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(100);
        executor.setMaxPoolSize(10);

        scheduler.setPoolSize(1);
    }
}
