package com.ga.notificationservice.consumer;

import com.ga.notificationservice.event.UserEmailNotificationEvent;
import com.ga.notificationservice.service.PersistentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final PersistentNotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events:notification-events}"
    )
    public void consume(String payload) {

        try {
            UserEmailNotificationEvent event =
                    objectMapper.readValue(
                            payload,
                            UserEmailNotificationEvent.class
                    );

            notificationService.createNotification(
                    event.eventId(),
                    null,
                    event.recipientEmail(),
                    null,
                    null,
                    event.eventType(),
                    event.subject(),
                    event.body(),
                    event.subject(),
                    event.body()
            );

            log.info(
                    "Processed user notification event: {}",
                    event.eventId()
            );

        } catch (RuntimeException exception) {
            log.error("Failed to process user notification event", exception);
            throw exception;
        }
    }
}
