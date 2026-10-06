package com.ga.medibook.service;

import com.ga.medibook.dto.request.AppointmentRequest;
import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.notification.EmailService;
import com.ga.medibook.notification.SseNotificationService;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private SseNotificationService sseNotificationService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void shouldRejectAppointmentOutsideDoctorAvailability() {

        // Patient
        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@medibook.com");
        patient.setRole(UserRole.PATIENT);
        patient.setStatus(UserStatus.ACTIVE);
        patient.setEmailVerified(true);

        // Doctor's user account
        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@medibook.com");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setStatus(UserStatus.ACTIVE);
        doctorUser.setEmailVerified(true);

        // Doctor
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        // Doctor is available from 09:00 to 11:00
        LocalDateTime availabilityStart =
                LocalDateTime.now()
                        .plusDays(1)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

        LocalDateTime availabilityEnd =
                availabilityStart.plusHours(2);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setStartDateTime(availabilityStart);
        availability.setEndDateTime(availabilityEnd);

        // Patient tries to book 12:00 to 13:00
        // This is outside the doctor's availability.
        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(
                availabilityStart.plusHours(3)
        );
        request.setEndDateTime(
                availabilityStart.plusHours(4)
        );
        request.setReason("General consultation");

        // Patient lookup
        doReturn(Optional.of(patient))
                .when(userRepository)
                .findByEmail("patient@medibook.com");

        // IMPORTANT:
        // AppointmentService uses the pessimistic-lock lookup
        // to prevent concurrent double-booking.
        doReturn(Optional.of(doctor))
                .when(doctorRepository)
                .findByIdForUpdate(10L);

        // Doctor availability
        doReturn(List.of(availability))
                .when(availabilityRepository)
                .findByDoctorId(10L);

        // Execute and expect the availability validation to fail.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        // Make sure it failed for the correct reason.
        assertEquals(
                "Appointment is outside the doctor's availability",
                exception.getMessage()
        );

        // Appointment must never be saved.
        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }
}