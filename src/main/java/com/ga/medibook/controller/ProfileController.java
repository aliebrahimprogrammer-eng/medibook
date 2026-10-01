package com.ga.medibook.controller;

import com.ga.medibook.dto.request.ProfileUpdateRequest;
import com.ga.medibook.dto.response.ProfileResponse;
import com.ga.medibook.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getMyProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                profileService.getMyProfile(
                        authentication.getName()
                )
        );
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {

        return ResponseEntity.ok(
                profileService.updateMyProfile(
                        authentication.getName(),
                        request
                )
        );
    }
}