
package com.ga.userservice.service;

import com.ga.userservice.dto.request.ForgotPasswordRequest;
import com.ga.userservice.dto.request.LoginRequest;
import com.ga.userservice.dto.request.RegisterRequest;
import com.ga.userservice.dto.request.ResetPasswordRequest;
import com.ga.userservice.dto.response.LoginResponse;
import com.ga.userservice.dto.response.UserResponse;
import com.ga.userservice.event.UserEmailNotificationEvent;
import com.ga.userservice.exception.ResourceConflictException;
import com.ga.userservice.model.entity.EmailVerificationToken;
import com.ga.userservice.model.entity.PasswordResetToken;
import com.ga.userservice.model.entity.User;
import com.ga.userservice.model.enums.UserRole;
import com.ga.userservice.model.enums.UserStatus;
import com.ga.userservice.repository.EmailVerificationTokenRepository;
import com.ga.userservice.repository.PasswordResetTokenRepository;
import com.ga.userservice.repository.UserProfileRepository;
import com.ga.userservice.repository.UserRepository;
import com.ga.userservice.security.JWTUtils;
import com.ga.userservice.security.MyUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("patient@example.com");
        registerRequest.setPassword("Password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Smith");
        registerRequest.setPhone("12345678");
    }

    @Test
    void registerShouldCreatePatientAndPublishVerificationEvent() {

        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("patient@example.com", response.getEmail());
        assertEquals(UserRole.PATIENT, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertFalse(response.isEmailVerified());

        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any());
        verify(emailVerificationTokenRepository).save(any());
        verify(auditLogService).log(
                any(User.class),
                eq("USER_REGISTERED"),
                eq("USER"),
                eq(1L),
                anyString()
        );
        verify(eventPublisher).publishEvent(
                any(UserEmailNotificationEvent.class)
        );
    }

    @Test
    void registerShouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> authService.register(registerRequest)
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(
                userProfileRepository,
                emailVerificationTokenRepository,
                eventPublisher
        );
    }

    @Test
    void loginShouldReturnJwtWhenAuthenticationSucceeds() {

        LoginRequest request = new LoginRequest();
        request.setEmail("patient@example.com");
        request.setPassword("Password123");

        MyUserDetails userDetails = mock(MyUserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(userDetails))
                .thenReturn("test-jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());

        verify(authenticationManager).authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));
        verify(jwtUtils).generateJwtToken(userDetails);
    }

    @Test
    void loginShouldPropagateAuthenticationFailure() {

        LoginRequest request = new LoginRequest();
        request.setEmail("patient@example.com");
        request.setPassword("WrongPassword");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenThrow(new RuntimeException("Authentication failed"));

        assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        verify(jwtUtils, never()).generateJwtToken(any());
    }


    @Test
    void verifyEmailShouldVerifyUserAndDeleteToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("patient@example.com");
        user.setEmailVerified(false);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(token));

        authService.verifyEmail("valid-token");

        assertTrue(user.isEmailVerified());

        verify(userRepository).save(user);
        verify(emailVerificationTokenRepository).delete(token);

        verify(auditLogService).log(
                eq(user),
                eq("USER_VERIFIED_EMAIL"),
                eq("USER"),
                eq(1L),
                anyString()
        );
    }

    @Test
    void verifyEmailShouldRejectInvalidToken() {

        when(emailVerificationTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyEmail("invalid-token")
        );

        assertEquals("Invalid verification token", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationTokenRepository, never())
                .delete(any(EmailVerificationToken.class));
    }

    @Test
    void verifyEmailShouldRejectExpiredToken() {

        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(emailVerificationTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyEmail("expired-token")
        );

        assertEquals(
                "Verification token has expired",
                exception.getMessage()
        );

        assertFalse(user.isEmailVerified());

        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationTokenRepository, never())
                .delete(any(EmailVerificationToken.class));
    }


    @Test
    void forgotPasswordShouldCreateTokenAndPublishNotification() {

        User user = new User();
        user.setId(1L);
        user.setEmail("patient@example.com");

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("patient@example.com");

        when(userRepository.findByEmail("patient@example.com"))
                .thenReturn(Optional.of(user));

        ReflectionTestUtils.setField(
                authService,
                "publicBaseUrl",
                "http://localhost:8081"
        );

        LocalDateTime before = LocalDateTime.now();

        authService.forgotPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNotNull(savedToken.getToken());
        assertEquals(user, savedToken.getUser());
        assertTrue(savedToken.getExpiresAt().isAfter(before.plusMinutes(29)));
        assertTrue(savedToken.getExpiresAt().isBefore(before.plusMinutes(31)));

        ArgumentCaptor<UserEmailNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(UserEmailNotificationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserEmailNotificationEvent event = eventCaptor.getValue();

        assertEquals("PASSWORD_RESET", event.eventType());
        assertEquals("patient@example.com", event.recipientEmail());
        assertTrue(event.body().contains(savedToken.getToken()));
        assertTrue(event.body().contains("http://localhost:8081/api/auth/reset-password"));

        verify(auditLogService).log(
                eq(user),
                eq("USER_FORGET_PASSWORD"),
                eq("USER"),
                eq(1L),
                anyString()
        );
    }

    @Test
    void forgotPasswordShouldRejectUnknownEmail() {

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.forgotPassword(request)
        );

        assertEquals("No user found with this email", exception.getMessage());

        verifyNoInteractions(
                passwordResetTokenRepository,
                eventPublisher,
                auditLogService
        );
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndDeleteToken() {

        User user = new User();
        user.setId(1L);
        user.setPassword("old-encoded-password");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-reset-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(20));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-reset-token");
        request.setNewPassword("NewPassword123");

        when(passwordResetTokenRepository.findByToken("valid-reset-token"))
                .thenReturn(Optional.of(token));

        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("new-encoded-password");

        authService.resetPassword(request);

        assertEquals("new-encoded-password", user.getPassword());

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);

        verify(auditLogService).log(
                eq(user),
                eq("PASSWORD_RESET"),
                eq("USER"),
                eq(1L),
                anyString()
        );
    }

    @Test
    void resetPasswordShouldRejectInvalidToken() {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("NewPassword123");

        when(passwordResetTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Invalid password reset token",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never())
                .delete(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordShouldRejectExpiredToken() {

        User user = new User();
        user.setId(1L);
        user.setPassword("old-encoded-password");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("NewPassword123");

        when(passwordResetTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Password reset token has expired",
                exception.getMessage()
        );

        assertEquals("old-encoded-password", user.getPassword());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never())
                .delete(any(PasswordResetToken.class));
    }


}
