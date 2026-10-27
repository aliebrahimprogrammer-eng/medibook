
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

    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";

    private final BackgroundJobRepository backgroundJobRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistRepository waitlistRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentEventService appointmentEventService;
    private final PersistentNotificationService persistentNotificationService;

    @Transactional
    public void processJob(Long jobId) {

        LocalDateTime now = LocalDateTime.now();

        // Atomically claim the job. Only one worker can succeed.
        int claimed = backgroundJobRepository.claimPendingJob(jobId, now);

        if (claimed == 0) {
            return;
        }

        BackgroundJob job = backgroundJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Background job not found: " + jobId
                        )
                );

        Appointment cancelledAppointment =
                job.getTriggeringAppointment();

        if (cancelledAppointment.getStatus()
                != AppointmentStatus.CANCELLED) {
            completeJob(job);
            return;
        }

        Long doctorId = cancelledAppointment.getDoctor().getId();

        // Lock the doctor while checking and allocating the slot.
        Doctor doctor = doctorRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found: " + doctorId
                        )
                );

        // Recheck that the released time slot is still free.
        List<Appointment> overlapping =
                appointmentRepository.findOverlappingAppointments(
                        doctorId,
                        cancelledAppointment.getStartDateTime(),
                        cancelledAppointment.getEndDateTime(),
                        AppointmentStatus.CANCELLED
                );

        if (!overlapping.isEmpty()) {
            completeJob(job);
            return;
        }

        // Find the earliest eligible waiting patient.
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
            appointmentEventService.recordAppointmentEvent(
                    cancelledAppointment,
                    "WAITLIST_NO_MATCH",
                    "No eligible waitlist entry found for job " + jobId
            );

            completeJob(job);
            return;
        }

        // Create the replacement appointment.
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

        // Mark the waitlist entry as matched.
        match.setStatus(WaitlistStatus.MATCHED);
        match.setMatchedAppointment(savedAppointment);
        waitlistRepository.save(match);

        // Record the allocation event.
        appointmentEventService.recordWaitlistEvent(
                match,
                "WAITLIST_MATCHED",
                "Waitlist entry matched to appointment "
                        + savedAppointment.getId()
        );

        // Persist an in-app notification and a pending email delivery.
        persistentNotificationService.createNotification(
                match.getPatient(),
                savedAppointment,
                match,
                "WAITLIST_MATCHED",
                "An appointment slot is available",
                "A slot has been allocated to you for "
                        + savedAppointment.getStartDateTime()
                        + " to "
                        + savedAppointment.getEndDateTime()
                        + ". Your appointment is currently PENDING."
        );

        completeJob(job);
    }

    private void completeJob(BackgroundJob job) {
        job.setStatus(COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setLastError(null);
        backgroundJobRepository.save(job);
    }
}
