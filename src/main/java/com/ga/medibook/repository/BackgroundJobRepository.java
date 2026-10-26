
package com.ga.medibook.repository;

import com.ga.medibook.model.entity.BackgroundJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackgroundJobRepository
        extends JpaRepository<BackgroundJob, Long> {

    List<BackgroundJob> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            String status,
            LocalDateTime scheduledAt,
            Pageable pageable
    );

    boolean existsByJobTypeAndTriggeringAppointmentId(
            String jobType,
            Long appointmentId
    );

    @Modifying
    @Query("""
        UPDATE BackgroundJob j
        SET j.status = 'PROCESSING',
            j.startedAt = :now,
            j.attemptCount = j.attemptCount + 1
        WHERE j.id = :jobId
          AND j.status = 'PENDING'
          AND j.scheduledAt <= :now
        """)
    int claimPendingJob(
            @Param("jobId") Long jobId,
            @Param("now") LocalDateTime now
    );
}
