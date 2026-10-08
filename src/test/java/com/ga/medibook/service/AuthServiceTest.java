package com.ga.medibook.service;

import com.ga.medibook.dto.request.LoginRequest;
import com.ga.medibook.dto.response.LoginResponse;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.security.JWTUtils;
import com.ga.medibook.security.MyUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLoginSuccessfullyAndReturnJwt() {

        LoginRequest request = new LoginRequest();
        request.setEmail("patient@medibook.com");
        request.setPassword("Patient123");

        User user = new User();
        user.setId(1L);
        user.setEmail("patient@medibook.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.PATIENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        MyUserDetails userDetails =
                new MyUserDetails(user);

        when(authenticationManager.authenticate(
                org.mockito.ArgumentMatchers.any(
                        UsernamePasswordAuthenticationToken.class
                )
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(jwtUtils.generateJwtToken(userDetails))
                .thenReturn("test-jwt-token");

        LoginResponse response =
                authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
    }
}