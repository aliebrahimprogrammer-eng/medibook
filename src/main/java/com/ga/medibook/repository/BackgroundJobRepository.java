package com.ga.medibook.repository;

import com.ga.medibook.model.entity.BackgroundJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
