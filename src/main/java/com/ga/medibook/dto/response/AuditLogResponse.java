package com.ga.medibook.dto.response;

import com.ga.medibook.model.enums.UserRole;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String userEmail,
        UserRole userRole,
        String action,
        String entityType,
        Long entityId,
        String description,
        LocalDateTime createdAt
) {
}