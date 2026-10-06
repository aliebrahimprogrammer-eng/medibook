package com.ga.medibook.security;

import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Test
    void shouldNotAuthenticateInactiveUser() throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        User user = new User();
        user.setId(1L);
        user.setEmail("inactive@medibook.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.PATIENT);
        user.setStatus(UserStatus.INACTIVE);
        user.setEmailVerified(true);

        MyUserDetails userDetails =
                new MyUserDetails(user);

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer test-token");

        when(jwtUtils.validateJwtToken("test-token"))
                .thenReturn(true);

        when(jwtUtils.getUserNameFromJwtToken("test-token"))
                .thenReturn("inactive@medibook.com");

        when(myUserDetailsService.loadUserByUsername(
                "inactive@medibook.com"
        )).thenReturn(userDetails);

        jwtRequestFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }
}