
package com.ga.userservice.security;

import com.ga.userservice.model.entity.User;
import com.ga.userservice.model.enums.UserRole;
import com.ga.userservice.model.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private MyUserDetailsService myUserDetailsService;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateInactiveUser() throws ServletException, IOException {
        assertUserIsNotAuthenticated(UserStatus.INACTIVE, true);
    }

    @Test
    void shouldNotAuthenticateUnverifiedUser() throws ServletException, IOException {
        assertUserIsNotAuthenticated(UserStatus.ACTIVE, false);
    }

    private void assertUserIsNotAuthenticated(
            UserStatus status,
            boolean emailVerified
    ) throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        User user = new User();
        user.setId(1L);
        user.setEmail("patient@medibook.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.PATIENT);
        user.setStatus(status);
        user.setEmailVerified(emailVerified);

        MyUserDetails userDetails = new MyUserDetails(user);

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer test-token");

        when(jwtUtils.validateJwtToken("test-token"))
                .thenReturn(true);

        when(jwtUtils.getUserNameFromJwtToken("test-token"))
                .thenReturn("patient@medibook.com");

        when(myUserDetailsService.loadUserByUsername(
                "patient@medibook.com"
        )).thenReturn(userDetails);

        jwtRequestFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }
}
