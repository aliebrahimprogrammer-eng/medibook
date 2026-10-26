package com.ga.medibook.service;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.model.entity.BackgroundJob;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.enums.AppointmentStatus;
import com.ga.medibook.model.enums.WaitlistStatus;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.AppointmentWaitlistRepository;
import com.ga.medibook.repository.BackgroundJobRepository;
import com.ga.medibook.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitlistAllocationProcessor {

    private static final String JOB_TYPE = "WAITLIST_ALLOCATION";
    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";

    private final BackgroundJobRepository backgroundJobRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistRepository waitlistRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentEventService appointmentEventService;

    @Transactional
    public void processJob(Long jobId) {

        BackgroundJob job = backgroundJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Background job not found: " + jobId
                        )
                );

        if (!PENDING.equals(job.getStatus())) {
            return;
        }

        job.setStatus(PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        backgroundJobRepository.save(job);

        try {
            Appointment cancelledAppointment =
                    job.getTriggeringAppointment();

            if (cancelledAppointment.getStatus()
                    != AppointmentStatus.CANCELLED) {
                job.setStatus(COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                backgroundJobRepository.save(job);
                return;
            }

            Long doctorId = cancelledAppointment.getDoctor().getId();

            // Serialize allocations for the same doctor.
            Doctor doctor = doctorRepository.findByIdForUpdate(doctorId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Doctor not found: " + doctorId
                            )
                    );

            List<Appointment> overlapping =
                    appointmentRepository.findOverlappingAppointments(
                            doctorId,
                            cancelledAppointment.getStartDateTime(),
                            cancelledAppointment.getEndDateTime(),
                            AppointmentStatus.CANCELLED
                    );

            if (!overlapping.isEmpty()) {
                job.setStatus(COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                backgroundJobRepository.save(job);
                return;
            }

            List<AppointmentWaitlist> candidates =
                    waitlistRepository
                            .findByDoctorIdAndStatusOrderByCreatedAtAsc(
                                    doctorId,
                                    WaitlistStatus.WAITING,
                                    PageRequest.of(0, 100)
                            );

            AppointmentWaitlist match = candidates.stream()
                    .filter(entry ->
                            !entry.getPreferredStartDateTime()
                                    .isAfter(cancelledAppointment.getStartDateTime())
                                    &&
                                    !entry.getPreferredEndDateTime()
                                            .isBefore(cancelledAppointment.getEndDateTime())
                    )
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                job.setStatus(COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                backgroundJobRepository.save(job);

                appointmentEventService.recordAppointmentEvent(
                        cancelledAppointment,
                        "WAITLIST_NO_MATCH",
                        "No eligible waitlist entry was found for job "
                                + job.getId()
                );
                return;
            }

            Appointment newAppointment = new Appointment();
            newAppointment.setPatient(match.getPatient());
            newAppointment.setDoctor(doctor);
            newAppointment.setStartDateTime(
                    cancelledAppointment.getStartDateTime()
            );
            newAppointment.setEndDateTime(
                    cancelledAppointment.getEndDateTime()
            );
            newAppointment.setStatus(AppointmentStatus.PENDING);
            newAppointment.setReason(
                    "Appointment allocated from the waitlist"
            );

            Appointment savedAppointment =
                    appointmentRepository.save(newAppointment);

            match.setStatus(WaitlistStatus.MATCHED);
            match.setMatchedAppointment(savedAppointment);
            waitlistRepository.save(match);

            appointmentEventService.recordWaitlistEvent(
                    match,
                    "WAITLIST_MATCHED",
                    "Waitlist entry matched to appointment "
                            + savedAppointment.getId()
            );

            job.setStatus(COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setLastError(null);
            backgroundJobRepository.save(job);

        } catch (RuntimeException exception) {
            // Record failure for investigation.
            // The transaction will roll back these changes if the
            // exception is rethrown.
            throw exception;
        }
    }
}
