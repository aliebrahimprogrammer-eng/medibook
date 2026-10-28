
package com.ga.medibook.service;

import com.ga.medibook.dto.request.AppointmentWaitlistRequest;
import com.ga.medibook.dto.response.AppointmentWaitlistResponse;
import com.ga.medibook.exception.ResourceConflictException;
import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.model.enums.WaitlistStatus;
import com.ga.medibook.repository.AppointmentWaitlistRepository;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.UserRepository;
import com.ga.medibook.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentWaitlistService {

    private final AppointmentWaitlistRepository waitlistRepository;
    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AppointmentEventService appointmentEventService;
    private final UserProfileRepository userProfileRepository;

    /**
     * Adds an eligible patient to a doctor's waitlist.
     */
    @Transactional
    public AppointmentWaitlistResponse joinWaitlist(
            String patientEmail,
            AppointmentWaitlistRequest request
    ) {
        if (request == null
                || request.getDoctorId() == null
                || request.getPreferredStartDateTime() == null
                || request.getPreferredEndDateTime() == null) {
            throw new IllegalArgumentException(
                    "Doctor ID and preferred start and end times are required"
            );
        }

        LocalDateTime start = request.getPreferredStartDateTime();
        LocalDateTime end = request.getPreferredEndDateTime();

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Preferred end date and time must be after start date and time"
            );
        }

        if (!start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Preferred appointment time must be in the future"
            );
        }

        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Patient not found")
                );

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException(
                    "Only patients can join an appointment waitlist"
            );
        }

        if (patient.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Inactive users cannot join an appointment waitlist"
            );
        }

        if (!patient.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Email must be verified before joining a waitlist"
            );
        }

        // Lock the doctor row to serialize waitlist changes for this doctor.
        Doctor doctor = doctorRepository.findByIdForUpdate(
                request.getDoctorId()
        ).orElseThrow(() ->
                new IllegalArgumentException("Doctor not found")
        );

        if (doctor.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Doctor is inactive");
        }

        List<Availability> availabilities =
                availabilityRepository.findByDoctorId(doctor.getId());

        boolean fitsAvailability = availabilities.stream()
                .anyMatch(availability ->
                        !start.isBefore(availability.getStartDateTime())
                                && !end.isAfter(availability.getEndDateTime())
                );

        if (!fitsAvailability) {
            throw new IllegalArgumentException(
                    "Requested time is outside the doctor's availability"
            );
        }

        boolean duplicate = waitlistRepository
                .existsByPatientIdAndDoctorIdAndPreferredStartDateTimeAndPreferredEndDateTimeAndStatus(
                        patient.getId(),
                        doctor.getId(),
                        start,
                        end,
                        WaitlistStatus.WAITING
                );

        if (duplicate) {
            throw new ResourceConflictException(
                    "You already have a waiting request for this doctor and time"
            );
        }

        AppointmentWaitlist entry = new AppointmentWaitlist();
        entry.setPatient(patient);
        entry.setDoctor(doctor);
        entry.setPreferredStartDateTime(start);
        entry.setPreferredEndDateTime(end);
        entry.setStatus(WaitlistStatus.WAITING);

        AppointmentWaitlist saved = waitlistRepository.save(entry);

        auditLogService.log(
                patient,
                "JOIN_APPOINTMENT_WAITLIST",
                "APPOINTMENT_WAITLIST",
                saved.getId(),
                "Patient joined the waitlist for doctor ID "
                        + doctor.getId()
                        + " for "
                        + start
                        + " to "
                        + end
        );

        appointmentEventService.recordWaitlistEvent(
                saved,
                "WAITLIST_JOINED",
                "Patient " + patient.getId()
                        + " joined the waitlist for doctor "
                        + doctor.getId()
                        + " for " + start + " to " + end
        );

        return toResponse(saved);
    }

    /**
     * Returns the authenticated patient's waitlist entries.
     */
    @Transactional(readOnly = true)
    public List<AppointmentWaitlistResponse> findMyWaitlist(
            String patientEmail
    ) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Patient not found")
                );

        return waitlistRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cancels a patient's waiting request.
     */
    @Transactional
    public void cancelWaitlistEntry(
            Long waitlistEntryId,
            String patientEmail
    ) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Patient not found")
                );

        AppointmentWaitlist entry = waitlistRepository
                .findById(waitlistEntryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Waitlist entry not found"
                        )
                );

        if (!entry.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException(
                    "You can only cancel your own waitlist entries"
            );
        }

        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new ResourceConflictException(
                    "Only waiting entries can be cancelled"
            );
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        AppointmentWaitlist saved = waitlistRepository.save(entry);

        auditLogService.log(
                patient,
                "CANCEL_APPOINTMENT_WAITLIST",
                "APPOINTMENT_WAITLIST",
                saved.getId(),
                "Patient cancelled waitlist entry " + saved.getId()
        );

        appointmentEventService.recordWaitlistEvent(
                saved,
                "WAITLIST_CANCELLED",
                "Patient " + patient.getId()
                        + " cancelled waitlist entry " + saved.getId()
        );
    }

    private AppointmentWaitlistResponse toResponse(
            AppointmentWaitlist entry
    ) {
        Long doctorUserId = entry.getDoctor().getUser().getId();

        String doctorName = userProfileRepository
                .findByUserId(doctorUserId)
                .map(profile ->
                        profile.getFirstName() + " " + profile.getLastName()
                )
                .orElse("Unknown Doctor");

        return new AppointmentWaitlistResponse(
                entry.getId(),
                entry.getPatient().getId(),
                entry.getDoctor().getId(),
                doctorName,
                entry.getPreferredStartDateTime(),
                entry.getPreferredEndDateTime(),
                entry.getStatus(),
                entry.getMatchedAppointment() == null
                        ? null
                        : entry.getMatchedAppointment().getId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
