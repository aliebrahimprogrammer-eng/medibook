package com.ga.notificationservice.cucumber;

import com.ga.notificationservice.notification.EmailService;
import com.ga.notificationservice.notification.SseNotificationService;
import io.cucumber.java.Before;
import org.mockito.Mockito;

public class CucumberHooks {

    private final EmailService emailService;
    private final SseNotificationService sseNotificationService;

    public CucumberHooks(
            EmailService emailService,
            SseNotificationService sseNotificationService
    ) {
        this.emailService = emailService;
        this.sseNotificationService = sseNotificationService;
    }

    @Before
    public void resetMocks() {
        Mockito.reset(emailService, sseNotificationService);
    }
}
