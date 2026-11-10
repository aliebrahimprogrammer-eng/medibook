package com.ga.notificationservice.cucumber;

import com.ga.notificationservice.consumer.UserNotificationConsumer;
import com.ga.notificationservice.event.UserEmailNotificationEvent;
import com.ga.notificationservice.model.entity.Notification;
import com.ga.notificationservice.model.entity.NotificationDelivery;
import com.ga.notificationservice.notification.EmailService;
import com.ga.notificationservice.repository.NotificationDeliveryRepository;
import com.ga.notificationservice.repository.NotificationRepository;
import com.ga.notificationservice.service.NotificationDeliveryService;
import com.ga.notificationservice.service.PersistentNotificationService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

public class NotificationSteps {

    private final ObjectMapper objectMapper;
    private final UserNotificationConsumer userNotificationConsumer;
    private final PersistentNotificationService persistentNotificationService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final EmailService emailService;
    private final ScenarioState state;

    private String eventPayload;

    public NotificationSteps(
            ObjectMapper objectMapper,
            UserNotificationConsumer userNotificationConsumer,
            PersistentNotificationService persistentNotificationService,
            NotificationDeliveryService notificationDeliveryService,
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            EmailService emailService,
            ScenarioState state
    ) {
        this.objectMapper = objectMapper;
        this.userNotificationConsumer = userNotificationConsumer;
        this.persistentNotificationService = persistentNotificationService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.emailService = emailService;
        this.state = state;
    }

    // Scenario 1: Notification event persistence

    @Given("a valid user notification event exists")
    public void aValidUserNotificationEventExists() throws Exception {
        UUID eventId = UUID.randomUUID();

        UserEmailNotificationEvent event =
                new UserEmailNotificationEvent(
                        eventId,
                        "PASSWORD_RESET",
                        "bdd-test@example.com",
                        "Password reset",
                        "Your password reset request was received.",
                        Instant.now()
                );

        state.setEventId(eventId);
        eventPayload = objectMapper.writeValueAsString(event);
    }

    @When("the notification event is consumed")
    public void theNotificationEventIsConsumed() {
        userNotificationConsumer.consume(eventPayload);
    }

    @Then("the notification and email delivery should be saved with pending status")
    public void theNotificationAndEmailDeliveryShouldBeSavedWithPendingStatus() {
        Notification notification = notificationRepository.findAll()
                .stream()
                .filter(item -> state.getEventId().equals(item.getSourceEventId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Notification was not saved"));

        assertEquals("PENDING", notification.getStatus());
        assertEquals("bdd-test@example.com", notification.getRecipientEmail());

        NotificationDelivery delivery = deliveryRepository
                .findByNotificationIdOrderByCreatedAtDesc(notification.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email delivery was not saved"));

        assertEquals("EMAIL", delivery.getChannel());
        assertEquals("PENDING", delivery.getStatus());
        assertEquals(0, delivery.getAttemptCount());

        state.setNotificationId(notification.getId());
        state.setDeliveryId(delivery.getId());
    }

    // Scenario 2: Successful email delivery

    @Given("a notification with a pending email delivery exists")
    public void aNotificationWithAPendingEmailDeliveryExists() {
        Notification notification =
                persistentNotificationService.createNotification(
                        UUID.randomUUID(),
                        null,
                        "delivery-bdd@example.com",
                        null,
                        null,
                        "TEST_NOTIFICATION",
                        "BDD delivery test",
                        "Testing successful email delivery.",
                        "BDD email test",
                        "This email is generated by a Cucumber test."
                );

        assertNotNull(notification);

        NotificationDelivery delivery = deliveryRepository
                .findByNotificationIdOrderByCreatedAtDesc(notification.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email delivery was not created"));

        state.setNotificationId(notification.getId());
        state.setDeliveryId(delivery.getId());
    }

    @When("the notification delivery is processed")
    public void theNotificationDeliveryIsProcessed() {
        notificationDeliveryService.deliver(state.getDeliveryId());
    }

    @Then("the delivery status should be sent")
    public void theDeliveryStatusShouldBeSent() {
        NotificationDelivery delivery = deliveryRepository
                .findById(state.getDeliveryId())
                .orElseThrow(() -> new AssertionError("Delivery was not found"));

        assertEquals("SENT", delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertNotNull(delivery.getDeliveredAt());
        assertNull(delivery.getLastError());

        verify(emailService).sendEmail(
                "delivery-bdd@example.com",
                "BDD email test",
                "This email is generated by a Cucumber test."
        );
    }

    // Scenario 3: Stuck delivery recovery

    @Given("a notification delivery has been stuck processing for more than five minutes")
    public void aNotificationDeliveryHasBeenStuckProcessing() {
        Notification notification =
                persistentNotificationService.createNotification(
                        UUID.randomUUID(),
                        null,
                        "recovery-bdd@example.com",
                        null,
                        null,
                        "TEST_NOTIFICATION",
                        "BDD recovery test",
                        "Testing stuck delivery recovery.",
                        "BDD recovery email",
                        "This delivery should be recovered."
                );

        assertNotNull(notification);

        NotificationDelivery delivery = deliveryRepository
                .findByNotificationIdOrderByCreatedAtDesc(notification.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email delivery was not created"));

        delivery.setStatus("PROCESSING");
        delivery.setAttemptCount(1);
        delivery.setLastAttemptAt(LocalDateTime.now().minusMinutes(10));

        deliveryRepository.saveAndFlush(delivery);

        state.setNotificationId(notification.getId());
        state.setDeliveryId(delivery.getId());
    }

    @When("the stuck delivery recovery process runs")
    public void theStuckDeliveryRecoveryProcessRuns() {
        notificationDeliveryService.recoverStuckDeliveries();
    }

    @Then("the delivery should be marked as failed with a timeout error")
    public void theDeliveryShouldBeMarkedAsFailedWithATimeoutError() {
        NotificationDelivery delivery = deliveryRepository
                .findById(state.getDeliveryId())
                .orElseThrow(() -> new AssertionError("Delivery was not found"));

        assertEquals("FAILED", delivery.getStatus());
        assertEquals(
                "Delivery timed out while processing",
                delivery.getLastError()
        );
    }
}
