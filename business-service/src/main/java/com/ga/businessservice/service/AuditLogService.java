package com.ga.businessservice.service;

import com.ga.businessservice.dto.response.AuditLogResponse;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.entity.AuditLog;
import com.ga.businessservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(
            InternalUserResponse actor,
            String action,
            String entityType,
            Long entityId,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actor.id());
        auditLog.setActorEmail(actor.email());
        auditLog.setActorRole(actor.role());
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(String search, Pageable pageable) {
        Page<AuditLog> auditLogs;

        if (search == null || search.isBlank()) {
            auditLogs = auditLogRepository.findAll(pageable);
        } else {
            auditLogs = auditLogRepository.search(search.trim(), pageable);
        }

        return auditLogs.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorEmail(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getCreatedAt()
        );
    }
}