package com.ga.medibook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class WaitlistExecutorConfig {

    @Bean(name = "waitlistExecutor", destroyMethod = "shutdown")
    public ExecutorService waitlistExecutor() {

        AtomicInteger threadNumber = new AtomicInteger(1);

        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(
                    task,
                    "waitlist-worker-" + threadNumber.getAndIncrement()
            );
            thread.setDaemon(false);
            return thread;
        };

        return new ThreadPoolExecutor(
                2,                   // Core threads
                2,                              // Maximum threads
                0L,                             // Keep-alive time
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),  // Maximum queued tasks
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
