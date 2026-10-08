package com.ga.medibook.service;

import com.ga.medibook.dto.request.AppointmentRequest;
import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.AppointmentStatus;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.notification.EmailService;
import com.ga.medibook.notification.SseNotificationService;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private SseNotificationService sseNotificationService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void shouldRejectAppointmentOutsideDoctorAvailability() {

        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@medibook.com");
        patient.setRole(UserRole.PATIENT);
        patient.setStatus(UserStatus.ACTIVE);
        patient.setEmailVerified(true);

        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@medibook.com");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setStatus(UserStatus.ACTIVE);
        doctorUser.setEmailVerified(true);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

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

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(availabilityStart.plusHours(3));
        request.setEndDateTime(availabilityStart.plusHours(4));
        request.setReason("General consultation");

        doReturn(Optional.of(patient))
                .when(userRepository)
                .findByEmail("patient@medibook.com");

        doReturn(Optional.of(doctor))
                .when(doctorRepository)
                .findByIdForUpdate(10L);

        doReturn(List.of(availability))
                .when(availabilityRepository)
                .findByDoctorId(10L);

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }

    @Test
    void shouldRejectDoubleBooking() {

        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@medibook.com");
        patient.setRole(UserRole.PATIENT);
        patient.setStatus(UserStatus.ACTIVE);
        patient.setEmailVerified(true);

        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@medibook.com");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setStatus(UserStatus.ACTIVE);
        doctorUser.setEmailVerified(true);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        LocalDateTime start =
                LocalDateTime.now()
                        .plusDays(1)
                        .withHour(10)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

        LocalDateTime end = start.plusHours(1);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setStartDateTime(start.minusHours(1));
        availability.setEndDateTime(end.plusHours(1));

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(100L);
        existingAppointment.setDoctor(doctor);
        existingAppointment.setPatient(patient);
        existingAppointment.setStartDateTime(start);
        existingAppointment.setEndDateTime(end);
        existingAppointment.setStatus(AppointmentStatus.CONFIRMED);

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(start);
        request.setEndDateTime(end);
        request.setReason("General consultation");

        doReturn(Optional.of(patient))
                .when(userRepository)
                .findByEmail("patient@medibook.com");

        doReturn(Optional.of(doctor))
                .when(doctorRepository)
                .findByIdForUpdate(10L);

        doReturn(List.of(availability))
                .when(availabilityRepository)
                .findByDoctorId(10L);

        doReturn(List.of(existingAppointment))
                .when(appointmentRepository)
                .findOverlappingAppointments(
                        10L,
                        start,
                        end,
                        AppointmentStatus.CANCELLED
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }

    @Test
    void shouldAllowPatientToCancelOwnAppointment() {

        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@medibook.com");
        patient.setRole(UserRole.PATIENT);
        patient.setStatus(UserStatus.ACTIVE);
        patient.setEmailVerified(true);

        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@medibook.com");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setStatus(UserStatus.ACTIVE);
        doctorUser.setEmailVerified(true);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStartDateTime(
                LocalDateTime.now().plusDays(1)
        );
        appointment.setEndDateTime(
                LocalDateTime.now().plusDays(1).plusHours(1)
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setReason("General consultation");

        doReturn(Optional.of(patient))
                .when(userRepository)
                .findByEmail("patient@medibook.com");

        doReturn(Optional.of(appointment))
                .when(appointmentRepository)
                .findById(100L);

        doReturn(Optional.of(doctor))
                .when(doctorRepository)
                .findById(10L);

        appointmentService.cancelAppointment(
                100L,
                "patient@medibook.com"
        );

        assertEquals(
                AppointmentStatus.CANCELLED,
                appointment.getStatus()
        );

        verify(appointmentRepository)
                .save(appointment);
    }

    @Test
    void shouldRejectInactivePatientFromCreatingAppointment() {

        User inactivePatient = new User();
        inactivePatient.setId(1L);
        inactivePatient.setEmail("patient@medibook.com");
        inactivePatient.setRole(UserRole.PATIENT);
        inactivePatient.setStatus(UserStatus.INACTIVE);
        inactivePatient.setEmailVerified(true);

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);

        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        request.setStartDateTime(start);
        request.setEndDateTime(start.plusHours(1));
        request.setReason("General consultation");

        doReturn(Optional.of(inactivePatient))
                .when(userRepository)
                .findByEmail("patient@medibook.com");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        assertEquals(
                "Inactive users cannot create appointments",
                exception.getMessage()
        );

        verify(doctorRepository, never())
                .findByIdForUpdate(anyLong());

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }
}