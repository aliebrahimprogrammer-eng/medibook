package com.ga.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserEmailNotificationEvent(
        UUID eventId,
        String eventType,
        String recipientEmail,
        String subject,
        String body,
        Instant createdAt
) {
}
