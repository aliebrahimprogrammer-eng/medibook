package com.ga.businessservice.repository;

import com.ga.businessservice.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE
            CAST(a.actorUserId AS string) LIKE CONCAT('%', :search, '%')
            OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.actorRole) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<AuditLog> search(
            @Param("search") String search,
            Pageable pageable
    );
}