package com.ga.userservice.repository;

import com.ga.userservice.model.entity.AuditLog;
import com.ga.userservice.model.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorUserIdIsNotNull(Pageable pageable);

    @Query("""
        SELECT a
        FROM AuditLog a
        LEFT JOIN UserProfile p ON p.user.id = a.actorUserId
        WHERE a.actorUserId IS NOT NULL
        AND (
            CAST(a.actorUserId AS string) LIKE CONCAT('%', :search, '%')
            OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.actorRole) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<AuditLog> search(
            @Param("search") String search,
            Pageable pageable
    );
}
