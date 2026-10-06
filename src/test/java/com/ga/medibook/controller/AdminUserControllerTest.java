package com.ga.medibook.controller;

import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.service.AdminUserService;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(AdminUserControllerTest.TestConfig.class)
class AdminUserControllerTest {

    @jakarta.annotation.Resource
    private AdminUserController adminUserController;

    @Test
    @WithMockUser(
            username = "patient@medibook.com",
            roles = "PATIENT"
    )
    void shouldRejectPatientFromAdminEndpoint() {

        assertThrows(
                AccessDeniedException.class,
                () -> adminUserController.findAllUsers()
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        AdminUserService adminUserService() {
            return org.mockito.Mockito.mock(AdminUserService.class);
        }

        @Bean
        AdminUserController adminUserController(
                AdminUserService adminUserService
        ) {
            return new AdminUserController(adminUserService);
        }
    }
}