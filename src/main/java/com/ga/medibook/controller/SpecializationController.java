package com.ga.medibook.controller;

import com.ga.medibook.dto.request.SpecializationRequest;
import com.ga.medibook.dto.response.SpecializationResponse;
import com.ga.medibook.service.SpecializationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(
        name = "Specializations",
        description = "Clinic specialization management"
)
@RestController
@RequestMapping("/api/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService specializationService;

    @Operation(
            summary = "Create specialization",
            description = "Creates a new medical specialization"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SpecializationResponse> create(
            @Valid @RequestBody SpecializationRequest request,
            Authentication authentication
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(specializationService.create(
                        request,
                        authentication.getName()
                ));
    }

    @Operation(
            summary = "Get all specializations",
            description = "Retrieves a list of all medical specializations"
    )
    @GetMapping
    public ResponseEntity<List<SpecializationResponse>> findAll() {

        return ResponseEntity.ok(
                specializationService.findAll()
        );
    }

    @Operation(
            summary = "Get specialization by ID",
            description = "Retrieves a medical specialization by its ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<SpecializationResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                specializationService.findById(id)
        );
    }
}