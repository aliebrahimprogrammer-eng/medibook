package com.ga.userservice.service;

import com.ga.userservice.dto.response.AuditLogResponse;
import com.ga.userservice.model.entity.AuditLog;
import com.ga.userservice.model.entity.User;
import com.ga.userservice.repository.AuditLogRepository;
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
            User user,
            String action,
            String entityType,
            Long entityId,
            String description
    ) {

        AuditLog auditLog = new AuditLog();

        if (user != null) {
            auditLog.setActorUserId(user.getId());
            auditLog.setActorEmail(user.getEmail());
            auditLog.setActorRole(
                    user.getRole() != null
                            ? user.getRole().name()
                            : null
            );
        }

        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(
            String search,
            Pageable pageable
    ) {

        Page<AuditLog> auditLogs;

        if (search == null || search.isBlank()) {
            auditLogs = auditLogRepository.findByActorUserIdIsNotNull(pageable);
        } else {
            auditLogs = auditLogRepository.search(
                    search.trim(),
                    pageable
            );
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
