package com.ga.medibook.controller;

import com.ga.medibook.dto.request.AppointmentRequest;
import com.ga.medibook.dto.request.AppointmentStatusRequest;
import com.ga.medibook.dto.response.AppointmentResponse;
import com.ga.medibook.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(
        name = "Appointments",
        description = "Patient and doctor appointment operations"
)
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(
            summary = "Create appointment",
            description = "Creates a new appointment for the authenticated patient"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            Authentication authentication,
            @Valid @RequestBody AppointmentRequest request
    ) {

        AppointmentResponse response =
                appointmentService.create(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get my appointments",
            description = "Retrieves a paginated list of appointments for the authenticated patient"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/my")
    public ResponseEntity<Page<AppointmentResponse>> myAppointments(
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "startDateTime",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                appointmentService.findPatientAppointments(
                        authentication.getName(),
                        pageable
                )
        );
    }

    @Operation(
            summary = "Get doctor appointments",
            description = "Retrieves a paginated list of appointments for the authenticated doctor"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/my")
    public ResponseEntity<Page<AppointmentResponse>> doctorAppointments(
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "startDateTime",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                appointmentService.findDoctorAppointments(
                        authentication.getName(),
                        pageable
                )
        );
    }

    @Operation(
            summary = "Cancel appointment",
            description = "Cancels an appointment belonging to the authenticated patient"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            Authentication authentication,
            @PathVariable Long id
    ) {

        appointmentService.cancelAppointment(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update appointment status",
            description = "Updates the status of an appointment for the authenticated doctor"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request
    ) {

        appointmentService.updateStatus(
                id,
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get appointment by ID",
            description = "Retrieves an appointment by its ID for an authorized user"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> findById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                appointmentService.findById(
                        id,
                        authentication.getName()
                )
        );
    }
}