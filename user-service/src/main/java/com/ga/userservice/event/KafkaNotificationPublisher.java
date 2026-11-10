package com.ga.userservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(
        name = "app.kafka.notifications.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Component
@RequiredArgsConstructor
public class KafkaNotificationPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaNotificationPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(UserEmailNotificationEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    notificationTopic,
                    event.eventId().toString(),
                    payload
            ).whenComplete((result, exception) -> {

                if (exception != null) {
                    log.error(
                            "Failed to publish notification event {}",
                            event.eventId(),
                            exception
                    );
                } else {
                    log.info(
                            "Published notification event {} to topic {}",
                            event.eventId(),
                            notificationTopic
                    );
                }
            });

        } catch (Exception exception) {
            log.error(
                    "Could not serialize notification event {}",
                    event.eventId(),
                    exception
            );
        }
    }
}
