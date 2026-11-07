package com.ga.businessservice.service;

import com.ga.businessservice.dto.event.AppointmentNotificationEvent;
import com.ga.businessservice.dto.request.AppointmentRequest;
import com.ga.businessservice.dto.request.AppointmentStatusRequest;
import com.ga.businessservice.dto.response.AppointmentResponse;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.entity.Appointment;
import com.ga.businessservice.entity.Availability;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.exception.ResourceConflictException;
import com.ga.businessservice.model.enums.AppointmentStatus;
import com.ga.businessservice.repository.AppointmentRepository;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final UserServiceClient userServiceClient;
    private final AuditLogService auditLogService;
    private final AppointmentEventService appointmentEventService;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final BackgroundJobService backgroundJobService;

    private static final ConcurrentMap<Long, ReentrantLock> DOCTOR_BOOKING_LOCKS =
            new ConcurrentHashMap<>();

    @Transactional
    public AppointmentResponse create(
            String patientEmail,
            AppointmentRequest request
    ) {

        // 1. Find the authenticated patient through User Service.
        InternalUserResponse patient =
                userServiceClient.findByEmail(patientEmail);

        if (!"PATIENT".equals(patient.role())) {
            throw new IllegalArgumentException(
                    "Only patients can create appointments"
            );
        }

        if (!"ACTIVE".equals(patient.status())) {
            throw new IllegalArgumentException(
                    "Inactive users cannot create appointments"
            );
        }

        if (!patient.emailVerified()) {
            throw new IllegalArgumentException(
                    "Email must be verified before booking an appointment"
            );
        }

        // 2. Validate appointment time.
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new IllegalArgumentException(
                    "End date and time must be after start date and time"
            );
        }

        if (request.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Appointment cannot be in the past"
            );
        }

        // 3. Acquire the JVM-level lock for this doctor.
        // Keep the lock until the transaction commits or rolls back.
        ReentrantLock doctorBookingLock = DOCTOR_BOOKING_LOCKS.computeIfAbsent(
                request.getDoctorId(),
                ignored -> new ReentrantLock()
        );

        doctorBookingLock.lock();

        try {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            doctorBookingLock.unlock();
                        }
                    }
            );
        } catch (RuntimeException exception) {
            doctorBookingLock.unlock();
            throw exception;
        }

        // 4. Find the doctor while holding both locks.
        Doctor doctor = doctorRepository.findByIdForUpdate(
                request.getDoctorId()
        ).orElseThrow(() ->
                new IllegalArgumentException("Doctor not found")
        );

        // 5. Check doctor availability.
        List<Availability> availabilities =
                availabilityRepository.findByDoctorId(doctor.getId());

        boolean fitsAvailability = availabilities
                .stream()
                .anyMatch(availability ->
                        !request.getStartDateTime()
                                .isBefore(availability.getStartDateTime())
                                &&
                        !request.getEndDateTime()
                                .isAfter(availability.getEndDateTime())
                );

        if (!fitsAvailability) {
            throw new IllegalArgumentException(
                    "Appointment is outside the doctor's availability"
            );
        }

        // 6. Check for overlapping appointments.
        List<Appointment> overlappingAppointments =
                appointmentRepository.findOverlappingAppointments(
                        doctor.getId(),
                        request.getStartDateTime(),
                        request.getEndDateTime(),
                        AppointmentStatus.CANCELLED
                );

        if (!overlappingAppointments.isEmpty()) {
            throw new ResourceConflictException(
                    "Doctor already has an appointment during this time"
            );
        }

        // 7. Create appointment using the patient ID.
        Appointment appointment = new Appointment();

        appointment.setPatientId(patient.id());
        appointment.setDoctor(doctor);
        appointment.setStartDateTime(request.getStartDateTime());
        appointment.setEndDateTime(request.getEndDateTime());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReason(request.getReason());

        Appointment saved = appointmentRepository.save(appointment);

        // 8. Record appointment history.
        appointmentEventService.recordAppointmentEvent(
                saved,
                "APPOINTMENT_CREATED",
                "Patient created an appointment with doctor ID "
                        + doctor.getId()
        );

        // 9. Audit log.
        auditLogService.log(
                patient,
                "CREATE_APPOINTMENT",
                "APPOINTMENT",
                saved.getId(),
                "Patient created an appointment with doctor ID "
                        + doctor.getId()
        );

        // 10. Prepare patient and doctor notifications.
        InternalUserResponse doctorUser =
                userServiceClient.findById(doctor.getUserId());

        String patientMessage = "Appointment #" + saved.getId()
                + " was successfully created.";

        String doctorMessage = "A new appointment #" + saved.getId()
                + " has been booked.";

        publishNotification(
                "APPOINTMENT_CREATED",
                saved,
                patient,
                "MediBook Appointment Request",
                "Your appointment request has been created.\n\n"
                        + "Doctor ID: " + doctor.getId() + "\n"
                        + "Start: " + request.getStartDateTime() + "\n"
                        + "End: " + request.getEndDateTime() + "\n"
                        + "Status: PENDING",
                patientMessage
        );

        publishNotification(
                "APPOINTMENT_CREATED",
                saved,
                doctorUser,
                null,
                null,
                doctorMessage
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> findPatientAppointments(
            String patientEmail,
            Pageable pageable
    ) {

        InternalUserResponse patient =
                userServiceClient.findByEmail(patientEmail);

        return appointmentRepository
                .findByPatientId(patient.id(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> findDoctorAppointments(
            String doctorEmail,
            Pageable pageable
    ) {

        InternalUserResponse doctorUser =
                userServiceClient.findByEmail(doctorEmail);

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.id()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Doctor profile not found"
                )
        );

        return appointmentRepository
                .findByDoctorId(doctor.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void cancelAppointment(
            Long appointmentId,
            String patientEmail
    ) {

        InternalUserResponse patient =
                userServiceClient.findByEmail(patientEmail);

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        Doctor doctor = doctorRepository.findById(
                appointment.getDoctor().getId()
        ).orElseThrow(() ->
                new IllegalArgumentException("Doctor not found")
        );

        // Ownership check.
        if (!appointment.getPatientId().equals(patient.id())) {
            throw new IllegalArgumentException(
                    "You can only cancel your own appointments"
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Appointment is already cancelled"
            );
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Completed appointments cannot be cancelled"
            );
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        appointmentRepository.save(appointment);

        backgroundJobService.scheduleWaitlistAllocation(
                appointment.getId()
        );

        appointmentEventService.recordAppointmentEvent(
                appointment,
                "APPOINTMENT_CANCELLED",
                "Patient cancelled appointment"
        );

        auditLogService.log(
                patient,
                "CANCEL_APPOINTMENT",
                "APPOINTMENT",
                appointment.getId(),
                "Patient cancelled appointment"
        );

        InternalUserResponse doctorUser =
                userServiceClient.findById(doctor.getUserId());

        publishNotification(
                "APPOINTMENT_CANCELLED",
                appointment,
                patient,
                "MediBook Appointment Cancelled",
                "Your appointment has been cancelled.",
                "Appointment #" + appointment.getId()
                        + " was cancelled."
        );

        publishNotification(
                "APPOINTMENT_CANCELLED",
                appointment,
                doctorUser,
                null,
                null,
                "A new appointment #" + appointment.getId()
                        + " has been cancelled."
        );
    }

    @Transactional
    public void updateStatus(
            Long appointmentId,
            String doctorEmail,
            AppointmentStatusRequest request
    ) {

        InternalUserResponse doctorUser =
                userServiceClient.findByEmail(doctorEmail);

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.id()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Doctor profile not found"
                )
        );

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        // Make sure this doctor owns the appointment.
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException(
                    "You can only manage your own appointments"
            );
        }

        AppointmentStatus currentStatus = appointment.getStatus();
        AppointmentStatus newStatus = request.getStatus();

        // Preserve the original status-transition rules.
        if (currentStatus == AppointmentStatus.PENDING
                && newStatus == AppointmentStatus.CONFIRMED) {

            appointment.setStatus(newStatus);

        } else if (currentStatus == AppointmentStatus.PENDING
                && newStatus == AppointmentStatus.CANCELLED) {

            appointment.setStatus(newStatus);

        } else if (currentStatus == AppointmentStatus.CONFIRMED
                && newStatus == AppointmentStatus.COMPLETED) {

            appointment.setStatus(newStatus);

        } else if (currentStatus == AppointmentStatus.CONFIRMED
                && newStatus == AppointmentStatus.CANCELLED) {

            appointment.setStatus(newStatus);

        } else {
            throw new IllegalArgumentException(
                    "Invalid appointment status transition from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }

        appointmentRepository.save(appointment);

        if (newStatus == AppointmentStatus.CANCELLED) {
            backgroundJobService.scheduleWaitlistAllocation(
                    appointment.getId()
            );
        }

        appointmentEventService.recordAppointmentEvent(
                appointment,
                "APPOINTMENT_" + newStatus.name(),
                "Appointment status changed from "
                        + currentStatus + " to " + newStatus
        );

        auditLogService.log(
                doctorUser,
                "UPDATE_APPOINTMENT_STATUS",
                "APPOINTMENT",
                appointment.getId(),
                "Appointment status changed from "
                        + currentStatus
                        + " to "
                        + newStatus
        );

        InternalUserResponse patient =
                userServiceClient.findById(appointment.getPatientId());

        String patientMessage;
        String doctorMessage;
        String emailSubject;
        String emailBody;

        if (newStatus == AppointmentStatus.CONFIRMED) {

            emailSubject = "MediBook Appointment confirmed";

            patientMessage = "Your appointment has been confirmed.\n\n"
                    + "Doctor ID: " + doctor.getId() + "\n"
                    + "Start: " + appointment.getStartDateTime() + "\n"
                    + "End: " + appointment.getEndDateTime() + "\n"
                    + "Status: CONFIRMED";

            emailBody = patientMessage;

            doctorMessage = "The appointment " + appointmentId
                    + "has been confirmed.\n\n"
                    + "Patient ID: " + appointment.getPatientId() + "\n"
                    + "Start: " + appointment.getStartDateTime() + "\n"
                    + "End: " + appointment.getEndDateTime() + "\n"
                    + "Status: CONFIRMED";

        } else if (newStatus == AppointmentStatus.COMPLETED) {

            emailSubject = "MediBook Appointment completed";

            patientMessage = "Your appointment has been completed.\n\n"
                    + "Doctor ID: " + doctor.getId() + "\n"
                    + "Start: " + appointment.getStartDateTime() + "\n"
                    + "End: " + appointment.getEndDateTime() + "\n"
                    + "Status: COMPLETED";

            emailBody = patientMessage;

            doctorMessage = "The appointment " + appointmentId
                    + "has been completed.\n\n"
                    + "Patient ID: " + appointment.getPatientId() + "\n"
                    + "Start: " + appointment.getStartDateTime() + "\n"
                    + "End: " + appointment.getEndDateTime() + "\n"
                    + "Status: COMPLETED";

        } else {

            emailSubject = "MediBook Appointment Cancelled";
            emailBody = "Your appointment has been cancelled.";

            patientMessage = "Appointment #" + appointment.getId()
                    + " was cancelled.";

            doctorMessage = "A new appointment #" + appointment.getId()
                    + " has been cancelled.";
        }

        publishNotification(
                "APPOINTMENT_" + newStatus.name(),
                appointment,
                patient,
                emailSubject,
                emailBody,
                patientMessage
        );

        InternalUserResponse doctorNotificationUser =
                userServiceClient.findById(doctor.getUserId());

        publishNotification(
                "APPOINTMENT_" + newStatus.name(),
                appointment,
                doctorNotificationUser,
                null,
                null,
                doctorMessage
        );
    }

    private AppointmentResponse toResponse(Appointment appointment) {

        InternalUserResponse doctorUser =
                userServiceClient.findById(
                        appointment.getDoctor().getUserId()
                );

        InternalUserResponse patientUser =
                userServiceClient.findById(
                        appointment.getPatientId()
                );

        String doctorName = doctorUser.firstName()
                + " "
                + doctorUser.lastName();

        String patientName = patientUser.firstName()
                + " "
                + patientUser.lastName();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctor().getId(),
                doctorName,
                patientName,
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(
            Long appointmentId,
            String userEmail
    ) {

        InternalUserResponse user =
                userServiceClient.findByEmail(userEmail);

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        boolean isPatient =
                appointment.getPatientId().equals(user.id());

        boolean isDoctor =
                appointment.getDoctor().getUserId().equals(user.id());

        boolean isAdmin = "ADMIN".equals(user.role());

        if (!isPatient && !isDoctor && !isAdmin) {
            throw new IllegalArgumentException(
                    "You are not allowed to view this appointment"
            );
        }

        return toResponse(appointment);
    }

    private void publishNotification(
            String eventType,
            Appointment appointment,
            InternalUserResponse recipient,
            String emailSubject,
            String emailBody,
            String notificationMessage
    ) {

        AppointmentNotificationEvent event =
                new AppointmentNotificationEvent(
                        UUID.randomUUID(),
                        eventType,
                        appointment.getId(),
                        recipient.id(),
                        recipient.email(),
                        recipient.role(),
                        emailSubject,
                        emailBody,
                        notificationMessage,
                        LocalDateTime.now()
                );

        // Publish only after the database transaction commits.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            appointmentEventPublisher.publish(event);
                        }
                    }
            );

        } else {
            appointmentEventPublisher.publish(event);
        }
    }
}
