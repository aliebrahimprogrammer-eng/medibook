
package com.ga.userservice.controller;

import com.ga.userservice.dto.response.InternalUserResponse;
import com.ga.userservice.service.InternalUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final InternalUserService internalUserService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @GetMapping("/{id}")
    public InternalUserResponse findById(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        validateApiKey(apiKey);
        return internalUserService.findById(id);
    }

    @GetMapping("/by-email")
    public InternalUserResponse findByEmail(
            @RequestParam String email,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        validateApiKey(apiKey);
        return internalUserService.findByEmail(email);
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid internal API key"
            );
        }
    }
}
