package com.ga.businessservice.service;

import com.ga.businessservice.dto.request.AppointmentWaitlistRequest;
import com.ga.businessservice.dto.response.AppointmentWaitlistResponse;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.exception.ResourceConflictException;
import com.ga.businessservice.entity.AppointmentWaitlist;
import com.ga.businessservice.entity.Availability;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.model.enums.WaitlistStatus;
import com.ga.businessservice.repository.AppointmentWaitlistRepository;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
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
    private final UserServiceClient userServiceClient;
    private final AuditLogService auditLogService;
    private final AppointmentEventService appointmentEventService;

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

        InternalUserResponse patient = userServiceClient.findByEmail(patientEmail);

        if (!"PATIENT".equals(patient.role())) {
            throw new IllegalArgumentException(
                    "Only patients can join an appointment waitlist"
            );
        }

        if (!"ACTIVE".equals(patient.status())) {
            throw new IllegalArgumentException(
                    "Inactive users cannot join an appointment waitlist"
            );
        }

        if (!patient.emailVerified()) {
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

        InternalUserResponse doctorUser =
                userServiceClient.findById(doctor.getUserId());

        if (!"ACTIVE".equals(doctorUser.status())) {
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
                        patient.id(),
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
        entry.setPatientId(patient.id());
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
                "Patient " + patient.id()
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
        InternalUserResponse patient =
                userServiceClient.findByEmail(patientEmail);

        return waitlistRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.id())
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
        InternalUserResponse patient =
                userServiceClient.findByEmail(patientEmail);

        AppointmentWaitlist entry = waitlistRepository
                .findById(waitlistEntryId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Waitlist entry not found")
                );

        if (!entry.getPatientId().equals(patient.id())) {
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
                "Patient " + patient.id()
                        + " cancelled waitlist entry " + saved.getId()
        );
    }

    private AppointmentWaitlistResponse toResponse(
            AppointmentWaitlist entry
    ) {
        InternalUserResponse doctorUser =
                userServiceClient.findById(entry.getDoctor().getUserId());

        String doctorName = doctorUser.firstName() + " "
                + doctorUser.lastName();

        return new AppointmentWaitlistResponse(
                entry.getId(),
                entry.getPatientId(),
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