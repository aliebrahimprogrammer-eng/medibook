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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(
            summary = "Create availability",
            description = "Creates a new availability slot for the authenticated doctor"
    )
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

    @Operation(
            summary = "Get doctor availability",
            description = "Retrieves a paginated list of availability slots for a doctor"
    )
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Page<AvailabilityResponse>> findByDoctor(
            @PathVariable Long doctorId,
            @PageableDefault(
                    size = 10,
                    sort = "startDateTime",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                availabilityService.findByDoctor(
                        doctorId,
                        pageable
                )
        );
    }

    @Operation(
            summary = "Delete availability",
            description = "Deletes an availability slot belonging to the authenticated doctor"
    )
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