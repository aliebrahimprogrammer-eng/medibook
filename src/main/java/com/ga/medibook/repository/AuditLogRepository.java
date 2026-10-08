package com.ga.medibook.repository;

import com.ga.medibook.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a
        FROM AuditLog a
        JOIN a.user u
        JOIN UserProfile p ON p.user.id = u.id
        WHERE
            CAST(u.id AS string) LIKE CONCAT('%', :search, '%')
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR p.phone LIKE CONCAT('%', :search, '%')
        """)
    Page<AuditLog> search(
            @Param("search") String search,
            Pageable pageable
    );
}