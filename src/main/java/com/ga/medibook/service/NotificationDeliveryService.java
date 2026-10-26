package com.ga.medibook.service;

import com.ga.medibook.model.entity.Notification;
import com.ga.medibook.model.entity.NotificationDelivery;
import com.ga.medibook.notification.EmailService;
import com.ga.medibook.notification.SseNotificationService;
import com.ga.medibook.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private static final String PENDING = "PENDING";
    private static final String FAILED = "FAILED";
    private static final String PROCESSING = "PROCESSING";
    private static final String SENT = "SENT";

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationDeliveryRepository deliveryRepository;
    private final EmailService emailService;
    private final SseNotificationService sseNotificationService;

    @Scheduled(fixedDelay = 10000)
    public void processPendingDeliveries() {

        List<NotificationDelivery> pending =
                deliveryRepository.findByStatusOrderByCreatedAtAsc(
                        PENDING,
                        PageRequest.of(0, 20)
                );

        List<NotificationDelivery> failed =
                deliveryRepository.findByStatusOrderByCreatedAtAsc(
                        FAILED,
                        PageRequest.of(0, 20)
                );

        List<NotificationDelivery> deliveries =
                new java.util.ArrayList<>();

        deliveries.addAll(pending);
        failed.stream()
                .filter(delivery ->
                        delivery.getAttemptCount() < MAX_ATTEMPTS)
                .forEach(deliveries::add);

        for (NotificationDelivery delivery : deliveries) {
            try {
                deliver(delivery.getId());
            } catch (RuntimeException exception) {
                // One failed delivery must not stop the remaining batch.
            }
        }
    }

    @Transactional
    public void deliver(Long deliveryId) {

        NotificationDelivery delivery =
                deliveryRepository.findById(deliveryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Delivery not found: " + deliveryId
                                )
                        );

        if (SENT.equals(delivery.getStatus())
                || PROCESSING.equals(delivery.getStatus())
                || delivery.getAttemptCount() >= MAX_ATTEMPTS) {
            return;
        }

        delivery.setStatus(PROCESSING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(LocalDateTime.now());
        delivery.setLastError(null);
        deliveryRepository.save(delivery);

        Notification notification = delivery.getNotification();

        try {
            if ("EMAIL".equals(delivery.getChannel())) {
                emailService.sendEmail(
                        notification.getUser().getEmail(),
                        notification.getTitle(),
                        notification.getMessage()
                );
            }

            // SSE is best-effort: it reaches currently connected clients.
            sseNotificationService.sendNotification(
                    notification.getUser().getEmail(),
                    notification.getTitle() + ": "
                            + notification.getMessage()
            );

            delivery.setStatus(SENT);
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.setLastError(null);

        } catch (RuntimeException exception) {
            delivery.setStatus(FAILED);
            delivery.setLastError(
                    exception.getMessage() == null
                            ? "Notification delivery failed"
                            : exception.getMessage()
            );
        }

        deliveryRepository.save(delivery);
    }
}
