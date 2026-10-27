
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private static final String PENDING = "PENDING";
    private static final String FAILED = "FAILED";
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

        List<NotificationDelivery> deliveries = new ArrayList<>();
        deliveries.addAll(pending);

        failed.stream()
                .filter(delivery ->
                        delivery.getAttemptCount() < MAX_ATTEMPTS)
                .forEach(deliveries::add);

        for (NotificationDelivery delivery : deliveries) {
            try {
                deliver(delivery.getId());
            } catch (RuntimeException exception) {
                // One delivery failure must not stop the remaining batch.
                // The delivery remains PROCESSING if an unexpected failure
                // occurs after the claim; recovery can be added separately.
            }
        }
    }

    /**
     * Claims the delivery in the database before attempting delivery.
     * This method intentionally has no surrounding transaction:
     * the atomic claim must commit before external services are called.
     */
    public void deliver(Long deliveryId) {

        int claimed = deliveryRepository.claimPendingDelivery(
                deliveryId,
                LocalDateTime.now()
        );

        if (claimed == 0) {
            // Already claimed, sent, or out of retries.
            return;
        }

        NotificationDelivery delivery =
                deliveryRepository.findWithNotificationAndUserById(deliveryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Delivery not found: " + deliveryId
                                )
                        );

        Notification notification = delivery.getNotification();

        try {
            if ("EMAIL".equals(delivery.getChannel())) {
                emailService.sendEmail(
                        notification.getUser().getEmail(),
                        notification.getTitle(),
                        notification.getMessage()
                );
            }
        } catch (RuntimeException exception) {
            delivery.setStatus(FAILED);
            delivery.setLastError(
                    exception.getMessage() == null
                            ? "Email delivery failed"
                            : exception.getMessage()
            );

            deliveryRepository.save(delivery);
            return;
        }

        // SSE is best-effort. An SSE failure must not cause a successfully
        // sent email to be retried.
        try {
            sseNotificationService.sendNotification(
                    notification.getUser().getEmail(),
                    notification.getTitle() + ": "
                            + notification.getMessage()
            );
        } catch (RuntimeException ignored) {
            // No connected SSE client, or another SSE delivery error.
        }

        delivery.setStatus(SENT);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setLastError(null);

        deliveryRepository.save(delivery);
    }
}
