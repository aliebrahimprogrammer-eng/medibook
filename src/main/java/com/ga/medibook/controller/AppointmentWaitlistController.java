package com.ga.medibook.controller;

import com.ga.medibook.dto.request.AppointmentWaitlistRequest;
import com.ga.medibook.dto.response.AppointmentWaitlistResponse;
import com.ga.medibook.service.AppointmentWaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
@Tag(name = "Appointment Waitlist", description = "Manage patient appointment waitlist entries")
public class AppointmentWaitlistController {

    private final AppointmentWaitlistService waitlistService;

    @PostMapping
    @Operation(
            summary = "Join the appointment waitlist",
            description = "Allows an authenticated patient to request a preferred appointment time."
    )
    public ResponseEntity<AppointmentWaitlistResponse> joinWaitlist(
            @Valid @RequestBody AppointmentWaitlistRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AppointmentWaitlistResponse response =
                waitlistService.joinWaitlist(
                        userDetails.getUsername(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my waitlist entries",
            description = "Returns waitlist entries belonging to the authenticated patient."
    )
    public ResponseEntity<List<AppointmentWaitlistResponse>> getMyWaitlist(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<AppointmentWaitlistResponse> response =
                waitlistService.findMyWaitlist(
                        userDetails.getUsername()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel a waitlist entry",
            description = "Cancels one of the authenticated patient's entries that is still waiting."
    )
    public ResponseEntity<Void> cancelWaitlistEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        waitlistService.cancelWaitlistEntry(
                id,
                userDetails.getUsername()
        );

        return ResponseEntity.noContent().build();
    }
}