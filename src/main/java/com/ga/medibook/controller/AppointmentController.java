package com.ga.medibook.controller;

import com.ga.medibook.dto.request.AppointmentRequest;
import com.ga.medibook.dto.response.AppointmentResponse;
import com.ga.medibook.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

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

    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> myAppointments(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                appointmentService.findPatientAppointments(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/doctor/my")
    public ResponseEntity<List<AppointmentResponse>> doctorAppointments(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                appointmentService.findDoctorAppointments(
                        authentication.getName()
                )
        );
    }

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
}