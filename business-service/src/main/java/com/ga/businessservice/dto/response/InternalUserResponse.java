package com.ga.businessservice.dto.response;

public record InternalUserResponse(
        Long id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        String firstName,
        String lastName
) {}