package com.ga.medibook.controller;

import com.ga.medibook.dto.request.AvailabilityRequest;
import com.ga.medibook.dto.response.AvailabilityResponse;
import com.ga.medibook.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    public ResponseEntity<AvailabilityResponse> create(
            Authentication authentication,
            @Valid @RequestBody AvailabilityRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        availabilityService.create(
                                authentication.getName(),
                                request
                        )
                );
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AvailabilityResponse>> findByDoctor(
            @PathVariable Long doctorId
    ) {

        return ResponseEntity.ok(
                availabilityService.findByDoctor(doctorId)
        );
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {

        availabilityService.delete(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}