package com.ga.medibook.service;

import com.ga.medibook.model.entity.AuditLog;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ga.medibook.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

        auditLog.setUser(user);
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

            auditLogs = auditLogRepository.findAll(pageable);

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
                auditLog.getUser().getId(),
                auditLog.getUser().getEmail(),
                auditLog.getUser().getRole(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getCreatedAt()
        );
    }
}