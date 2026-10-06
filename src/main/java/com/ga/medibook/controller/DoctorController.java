package com.ga.medibook.controller;

import com.ga.medibook.dto.request.DoctorRequest;
import com.ga.medibook.dto.request.DoctorUpdateRequest;
import com.ga.medibook.dto.response.DoctorResponse;
import com.ga.medibook.service.DoctorService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(
        name = "Doctors",
        description = "Doctor management and doctor search operations"
)
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(
            summary = "Create doctor",
            description = "Creates a doctor profile for an existing DOCTOR user"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DoctorResponse> create(
            @Valid @RequestBody DoctorRequest request,
            Authentication authentication
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(doctorService.create(
                        request,
                        authentication.getName()
                ));
    }

    @Operation(
            summary = "Get doctor by ID",
            description = "Returns the details of a specific doctor"
    )
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                doctorService.findById(id)
        );
    }

    @Operation(
            summary = "Search doctors",
            description = "Search doctors by name and/or specialization with pagination and sorting"
    )
    @GetMapping
    public ResponseEntity<Page<DoctorResponse>> searchDoctors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialization,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(
                doctorService.searchDoctors(
                        name,
                        specialization,
                        pageable
                )
        );
    }

    @Operation(
            summary = "Update doctor",
            description = "Updates an existing doctor's specialization, license number, and bio"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DoctorUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorService.update(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

}