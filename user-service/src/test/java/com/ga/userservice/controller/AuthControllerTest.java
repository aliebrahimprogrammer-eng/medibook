package com.ga.userservice.controller;

import com.ga.userservice.security.JWTUtils;
import com.ga.userservice.security.MyUserDetailsService;
import com.ga.userservice.security.RateLimitService;
import com.ga.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void shouldRejectInvalidRegistrationRequest() throws Exception {

        String invalidRequest = """
                {
                    "email": "not-an-email",
                    "password": "123",
                    "firstName": "",
                    "lastName": ""
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}