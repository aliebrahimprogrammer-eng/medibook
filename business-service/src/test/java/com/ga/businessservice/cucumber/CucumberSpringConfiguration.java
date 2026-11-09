package com.ga.businessservice.cucumber;

import com.ga.businessservice.service.AppointmentEventPublisher;
import com.ga.businessservice.security.JWTUtils;
import com.ga.businessservice.service.UserServiceClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private AppointmentEventPublisher appointmentEventPublisher;
}

