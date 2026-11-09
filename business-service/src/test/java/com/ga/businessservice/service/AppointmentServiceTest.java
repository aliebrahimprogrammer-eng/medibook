package com.ga.businessservice.service;

import com.ga.businessservice.dto.request.AppointmentRequest;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.repository.AppointmentRepository;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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

    @Test
    void shouldRejectInactivePatientFromCreatingAppointment() {

        // Arrange: simulate an inactive patient returned by User Service.
        InternalUserResponse inactivePatient =
                new InternalUserResponse(
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

        // Act: attempt to create an appointment.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.create(
                        "patient@medibook.com",
                        request
                )
        );

        // Assert: the original business rule is preserved.
        assertEquals(
                "Inactive users cannot create appointments",
                exception.getMessage()
        );

        // The request must stop before doctor lookup or appointment creation.
        verify(doctorRepository, never())
                .findByIdForUpdate(anyLong());

        verify(appointmentRepository, never())
                .save(any());

        verifyNoInteractions(
                availabilityRepository,
                auditLogService,
                appointmentEventService,
                appointmentEventPublisher,
                backgroundJobService
        );
    }
}