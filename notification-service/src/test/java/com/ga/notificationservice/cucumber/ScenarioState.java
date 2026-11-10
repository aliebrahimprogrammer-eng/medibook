package com.ga.notificationservice.cucumber;

import org.springframework.stereotype.Component;
import io.cucumber.spring.ScenarioScope;

import java.util.UUID;

@Component
@ScenarioScope
public class ScenarioState {

    private UUID eventId;
    private Long notificationId;
    private Long deliveryId;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }
}
