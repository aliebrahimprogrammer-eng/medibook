package com.ga.medibook.service;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.BackgroundJob;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.BackgroundJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BackgroundJobService {

    private static final String WAITLIST_ALLOCATION_JOB =
            "WAITLIST_ALLOCATION";

    private static final String PENDING = "PENDING";

    private final AppointmentRepository appointmentRepository;
    private final BackgroundJobRepository backgroundJobRepository;
    private final AppointmentEventService appointmentEventService;

    @Transactional
    public void scheduleWaitlistAllocation(Long appointmentId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Appointment not found with ID: " + appointmentId
                        )
                );

        // Avoid scheduling the same job more than once.
        boolean alreadyScheduled =
                backgroundJobRepository
                        .existsByJobTypeAndTriggeringAppointmentId(
                                WAITLIST_ALLOCATION_JOB,
                                appointmentId
                        );

        if (alreadyScheduled) {
            return;
        }

        BackgroundJob job = new BackgroundJob();
        job.setJobType(WAITLIST_ALLOCATION_JOB);
        job.setTriggeringAppointment(appointment);
        job.setStatus(PENDING);
        job.setAttemptCount(0);
        job.setScheduledAt(LocalDateTime.now());

        BackgroundJob savedJob = backgroundJobRepository.save(job);

        appointmentEventService.recordAppointmentEvent(
                appointment,
                "ALLOCATION_JOB_CREATED",
                "Created waitlist allocation job with ID: "
                        + savedJob.getId()
        );
    }
}
