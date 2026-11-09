package com.ga.userservice.controller;

import com.ga.userservice.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig
@Import(AdminUserControllerTest.MethodSecurityTestConfig.class)
class AdminUserControllerTest {

    @Autowired
    private AdminUserController adminUserController;

    @Autowired
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(username = "patient@example.com", roles = "PATIENT")
    void patientShouldNotAccessAdminUserEndpoints() {

        assertThrows(
                AccessDeniedException.class,
                () -> adminUserController.findAllUsers()
        );

        verifyNoInteractions(adminUserService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        AdminUserService adminUserService() {
            return mock(AdminUserService.class);
        }

        @Bean
        AdminUserController adminUserController(
                AdminUserService adminUserService
        ) {
            return new AdminUserController(adminUserService);
        }
    }
}