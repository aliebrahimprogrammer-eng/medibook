package com.ga.medibook.service;

import com.ga.medibook.model.entity.BackgroundJob;
import com.ga.medibook.repository.BackgroundJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistJobDispatcher {

    private final BackgroundJobRepository backgroundJobRepository;
    private final WaitlistAllocationProcessor allocationProcessor;

    @Qualifier("waitlistExecutor")
    private final ExecutorService waitlistExecutor;

    @Scheduled(fixedDelay = 5000)
    public void dispatchPendingJobs() {

        List<BackgroundJob> pendingJobs =
                backgroundJobRepository
                        .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                                "PENDING",
                                LocalDateTime.now(),
                                PageRequest.of(0, 10)
                        );

        for (BackgroundJob job : pendingJobs) {
            try {
                waitlistExecutor.execute(
                        () -> allocationProcessor.processJob(job.getId())
                );
            } catch (java.util.concurrent.RejectedExecutionException exception) {
                log.warn(
                        "Executor rejected waitlist job {}; it remains pending for a later retry",
                        job.getId(),
                        exception
                );
                break;
            }
        }
    }
}
