package com.ga.medibook.controller;

import com.ga.medibook.dto.request.SpecializationRequest;
import com.ga.medibook.dto.response.SpecializationResponse;
import com.ga.medibook.service.SpecializationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService specializationService;

    @PostMapping
    public ResponseEntity<SpecializationResponse> create(
            @Valid @RequestBody SpecializationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(specializationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SpecializationResponse>> findAll() {

        return ResponseEntity.ok(
                specializationService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecializationResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                specializationService.findById(id)
        );
    }
}