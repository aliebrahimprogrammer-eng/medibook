package com.ga.businessservice.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String userEmail,
        String userRole,
        String action,
        String entityType,
        Long entityId,
        String description,
        LocalDateTime createdAt
) {}