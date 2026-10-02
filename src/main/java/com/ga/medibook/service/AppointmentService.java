package com.ga.medibook.service;

import com.ga.medibook.dto.request.AppointmentRequest;
import com.ga.medibook.dto.request.AppointmentStatusRequest;
import com.ga.medibook.dto.response.AppointmentResponse;
import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.AppointmentStatus;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public AppointmentResponse create(
            String patientEmail,
            AppointmentRequest request
    ) {

        // 1. Find the authenticated patient
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        )
                );

        // 2. Make sure the user is actually a patient
        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException(
                    "Only patients can create appointments"
            );
        }

        // 3. Make sure the patient is active
        if (patient.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Inactive users cannot create appointments"
            );
        }

        // 4. Make sure the patient verified their email
        if (!patient.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Email must be verified before booking an appointment"
            );
        }

        // 5. Validate appointment time
        if (!request.getEndDateTime()
                .isAfter(request.getStartDateTime())) {

            throw new IllegalArgumentException(
                    "End date and time must be after start date and time"
            );
        }

        if (request.getStartDateTime()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Appointment cannot be in the past"
            );
        }

        // 6. Find the doctor
        Doctor doctor = doctorRepository.findById(
                request.getDoctorId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Doctor not found"
                )
        );

        // 7. Doctor must be active
        if (doctor.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Doctor is inactive"
            );
        }

        // 8. Check doctor availability
        List<Availability> availabilities =
                availabilityRepository.findByDoctorId(
                        doctor.getId()
                );

        boolean fitsAvailability = availabilities
                .stream()
                .anyMatch(availability ->
                        !request.getStartDateTime()
                                .isBefore(
                                        availability.getStartDateTime()
                                )
                                &&
                                !request.getEndDateTime()
                                        .isAfter(
                                                availability.getEndDateTime()
                                        )
                );

        if (!fitsAvailability) {
            throw new IllegalArgumentException(
                    "Appointment is outside the doctor's availability"
            );
        }

        // 9. Check for overlapping appointments
        List<Appointment> overlappingAppointments =
                appointmentRepository.findOverlappingAppointments(
                        doctor.getId(),
                        request.getStartDateTime(),
                        request.getEndDateTime(),
                        AppointmentStatus.CANCELLED
                );

        if (!overlappingAppointments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Doctor already has an appointment during this time"
            );
        }

        // 10. Create appointment
        Appointment appointment = new Appointment();

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStartDateTime(
                request.getStartDateTime()
        );
        appointment.setEndDateTime(
                request.getEndDateTime()
        );
        appointment.setStatus(
                AppointmentStatus.PENDING
        );
        appointment.setReason(request.getReason());

        Appointment saved =
                appointmentRepository.save(appointment);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findPatientAppointments(
            String patientEmail
    ) {

        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        )
                );

        return appointmentRepository
                .findByPatientId(patient.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findDoctorAppointments(
            String doctorEmail
    ) {

        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        )
                );

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.getId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Doctor profile not found"
                )
        );

        return appointmentRepository
                .findByDoctorId(doctor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void cancelAppointment(
            Long appointmentId,
            String patientEmail
    ) {

        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        )
                );

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        // Ownership check
        if (!appointment.getPatient()
                .getId()
                .equals(patient.getId())) {

            throw new IllegalArgumentException(
                    "You can only cancel your own appointments"
            );
        }

        // Don't cancel something already cancelled
        if (appointment.getStatus() ==
                AppointmentStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Appointment is already cancelled"
            );
        }

        // Don't cancel completed appointments
        if (appointment.getStatus() ==
                AppointmentStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "Completed appointments cannot be cancelled"
            );
        }

        appointment.setStatus(
                AppointmentStatus.CANCELLED
        );

        appointmentRepository.save(appointment);
    }

    @Transactional
    public void updateStatus(
            Long appointmentId,
            String doctorEmail,
            AppointmentStatusRequest request
    ) {

        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        )
                );

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.getId()
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

        // Make sure this doctor owns the appointment
        if (!appointment.getDoctor()
                .getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You can only manage your own appointments"
            );
        }

        AppointmentStatus currentStatus =
                appointment.getStatus();

        AppointmentStatus newStatus =
                request.getStatus();

        // PENDING → CONFIRMED
        if (currentStatus == AppointmentStatus.PENDING
                && newStatus == AppointmentStatus.CONFIRMED) {

            appointment.setStatus(newStatus);

            // PENDING → CANCELLED
        } else if (currentStatus == AppointmentStatus.PENDING
                && newStatus == AppointmentStatus.CANCELLED) {

            appointment.setStatus(newStatus);

            // CONFIRMED → COMPLETED
        } else if (currentStatus == AppointmentStatus.CONFIRMED
                && newStatus == AppointmentStatus.COMPLETED) {

            appointment.setStatus(newStatus);

            // CONFIRMED → CANCELLED
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
    }

    private AppointmentResponse toResponse(
            Appointment appointment
    ) {

        UserProfile doctorProfile =
                userProfileRepository
                        .findByUserId(
                                appointment
                                        .getDoctor()
                                        .getUser()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Doctor profile not found"
                                )
                        );

        String doctorName =
                doctorProfile.getFirstName()
                        + " "
                        + doctorProfile.getLastName();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                doctorName,
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

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        boolean isPatient =
                appointment.getPatient()
                        .getId()
                        .equals(user.getId());

        boolean isDoctor =
                appointment.getDoctor()
                        .getUser()
                        .getId()
                        .equals(user.getId());

        boolean isAdmin =
                user.getRole() == UserRole.ADMIN;

        if (!isPatient && !isDoctor && !isAdmin) {
            throw new IllegalArgumentException(
                    "You are not allowed to view this appointment"
            );
        }

        return toResponse(appointment);
    }
}