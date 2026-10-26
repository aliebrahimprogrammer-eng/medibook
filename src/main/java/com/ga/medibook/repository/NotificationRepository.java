package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    long countByUserIdAndStatus(
            Long userId,
            String status
    );
}
