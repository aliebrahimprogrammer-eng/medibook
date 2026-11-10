package com.ga.notificationservice.cucumber;

import com.ga.notificationservice.notification.EmailService;
import com.ga.notificationservice.notification.SseNotificationService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SseNotificationService sseNotificationService;
}
