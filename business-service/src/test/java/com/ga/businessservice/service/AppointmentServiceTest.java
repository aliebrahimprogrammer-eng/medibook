package com.ga.businessservice.service;

import com.ga.businessservice.dto.request.AppointmentRequest;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.entity.Appointment;
import com.ga.businessservice.entity.Availability;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.repository.AppointmentRepository;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
import com.ga.businessservice.exception.ResourceConflictException;
import com.ga.businessservice.model.enums.AppointmentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AppointmentEventService appointmentEventService;

    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    @Mock
    private BackgroundJobService backgroundJobService;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync ->
                            sync.afterCompletion(
                                    TransactionSynchronization.STATUS_ROLLED_BACK
                            )
                    );

            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRejectInactivePatientFromCreatingAppointment() {

        InternalUserResponse inactivePatient = new InternalUserResponse(
                1L,
                "patient@medibook.com",
                "PATIENT",
                "INACTIVE",
                true,
                "Test",
                "Patient"
        );

        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(start);
        request.setEndDateTime(start.plusHours(1));
        request.setReason("General consultation");

        when(userServiceClient.findByEmail("patient@medibook.com"))
                .thenReturn(inactivePatient);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create("patient@medibook.com", request)
        );

        assertEquals(
                "Inactive users cannot create appointments",
                exception.getMessage()
        );

        verify(doctorRepository, never()).findByIdForUpdate(anyLong());
        verify(appointmentRepository, never()).save(any());

        verifyNoInteractions(
                availabilityRepository,
                auditLogService,
                appointmentEventService,
                appointmentEventPublisher,
                backgroundJobService
        );
    }

    @Test
    void shouldRejectAppointmentOutsideDoctorAvailability() {

        InternalUserResponse activePatient = new InternalUserResponse(
                1L,
                "patient@medibook.com",
                "PATIENT",
                "ACTIVE",
                true,
                "Test",
                "Patient"
        );

        LocalDateTime availabilityStart = LocalDateTime.now()
                .plusDays(1)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        LocalDateTime availabilityEnd = availabilityStart.plusHours(1);

        // Requested appointment is outside the doctor's 9:00–10:00 availability.
        LocalDateTime appointmentStart = availabilityStart.plusHours(2);

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(appointmentStart);
        request.setEndDateTime(appointmentStart.plusHours(1));
        request.setReason("General consultation");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUserId(20L);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setStartDateTime(availabilityStart);
        availability.setEndDateTime(availabilityEnd);

        when(userServiceClient.findByEmail("patient@medibook.com"))
                .thenReturn(activePatient);

        when(doctorRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(doctor));

        when(availabilityRepository.findByDoctorId(10L))
                .thenReturn(List.of(availability));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create("patient@medibook.com", request)
        );

        assertEquals(
                "Appointment is outside the doctor's availability",
                exception.getMessage()
        );

        verify(doctorRepository).findByIdForUpdate(10L);
        verify(availabilityRepository).findByDoctorId(10L);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectDoubleBooking() {

        InternalUserResponse activePatient = new InternalUserResponse(
                1L,
                "patient@medibook.com",
                "PATIENT",
                "ACTIVE",
                true,
                "Test",
                "Patient"
        );

        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        LocalDateTime end = start.plusHours(1);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUserId(20L);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setStartDateTime(start.minusHours(1));
        availability.setEndDateTime(end.plusHours(1));

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(100L);
        existingAppointment.setDoctor(doctor);
        existingAppointment.setPatientId(2L);
        existingAppointment.setStartDateTime(start);
        existingAppointment.setEndDateTime(end);
        existingAppointment.setStatus(AppointmentStatus.CONFIRMED);

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setStartDateTime(start);
        request.setEndDateTime(end);
        request.setReason("General consultation");

        when(userServiceClient.findByEmail("patient@medibook.com"))
                .thenReturn(activePatient);

        when(doctorRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(doctor));

        when(availabilityRepository.findByDoctorId(10L))
                .thenReturn(List.of(availability));

        when(appointmentRepository.findOverlappingAppointments(
                10L,
                start,
                end,
                AppointmentStatus.CANCELLED
        )).thenReturn(List.of(existingAppointment));

        assertThrows(
                ResourceConflictException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}