package com.ga.medibook.controller;

import com.ga.medibook.dto.request.DoctorRequest;
import com.ga.medibook.dto.response.DoctorResponse;
import com.ga.medibook.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    public ResponseEntity<DoctorResponse> create(
            @Valid @RequestBody DoctorRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(doctorService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                doctorService.findById(id)
        );
    }
}