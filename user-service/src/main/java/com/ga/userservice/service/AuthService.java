package com.ga.userservice.service;

import com.ga.userservice.dto.request.*;
import com.ga.userservice.dto.response.LoginResponse;
import com.ga.userservice.dto.response.UserResponse;
import com.ga.userservice.exception.ResourceConflictException;
import com.ga.userservice.model.entity.EmailVerificationToken;
import com.ga.userservice.model.entity.PasswordResetToken;
import com.ga.userservice.model.entity.User;
import com.ga.userservice.model.entity.UserProfile;
import com.ga.userservice.model.enums.UserRole;
import com.ga.userservice.model.enums.UserStatus;
import com.ga.userservice.repository.EmailVerificationTokenRepository;
import com.ga.userservice.repository.PasswordResetTokenRepository;
import com.ga.userservice.repository.UserProfileRepository;
import com.ga.userservice.repository.UserRepository;
import com.ga.userservice.security.JWTUtils;
import com.ga.userservice.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import com.ga.userservice.event.UserEmailNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTUtils jwtUtils;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException(
                    "Email is already registered"
            );
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(UserRole.PATIENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        //create email verification token
        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setUser(savedUser);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiresAt(
                LocalDateTime.now().plusHours(24)
        );

        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink =
                publicBaseUrl + "/api/auth/verify-email?token="
                        + verificationToken.getToken();

        eventPublisher.publishEvent(new UserEmailNotificationEvent(
                UUID.randomUUID(),
                "EMAIL_VERIFICATION",
                user.getEmail(),
                "Verify your MediBook account",
                "Welcome to MediBook!\n\n"
                        + "Please verify your email address by opening "
                        + "the following link:\n\n"
                        + verificationLink,
                Instant.now()
        ));

        UserProfile profile = new UserProfile();

        profile.setUser(savedUser);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());

        userProfileRepository.save(profile);

        auditLogService.log(
                user,
                "USER_REGISTERED",
                "USER",
                user.getId(),
                "New user registered with the id " + user.getId()
        );

        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getStatus(),
                savedUser.isEmailVerified()
        );
    }

    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        MyUserDetails myUserDetails =
                (MyUserDetails) authentication.getPrincipal();

        String jwt = jwtUtils.generateJwtToken(myUserDetails);

        return new LoginResponse(jwt);
    }

    @Transactional
    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid verification token"
                                )
                        );

        if (verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Verification token has expired"
            );
        }

        User user = verificationToken.getUser();

        user.setEmailVerified(true);

        userRepository.save(user);

        emailVerificationTokenRepository.delete(
                verificationToken
        );

        auditLogService.log(
                user,
                "USER_VERIFIED_EMAIL",
                "USER",
                user.getId(),
                "Email verified for the user with the id " + user.getId()
        );
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No user found with this email"
                        )
                );

        passwordResetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();

        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        passwordResetTokenRepository.save(resetToken);

        String resetLink =
                publicBaseUrl + "/api/auth/reset-password?token="
                        + resetToken.getToken();

        eventPublisher.publishEvent(new UserEmailNotificationEvent(
                UUID.randomUUID(),
                "PASSWORD_RESET",
                user.getEmail(),
                "MediBook Password Reset",
                "We received a request to reset your MediBook password.\n\n"
                        + "Use the following token to reset your password:\n\n"
                        + resetToken.getToken()
                        + "\n\nOr use this link:\n\n"
                        + resetLink,
                Instant.now()
        ));

        auditLogService.log(
                user,
                "USER_FORGET_PASSWORD",
                "USER",
                user.getId(),
                "The user with the id " + user.getId() + " forget password."
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(request.getToken())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid password reset token"
                                )
                        );

        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Password reset token has expired"
            );
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        auditLogService.log(
                user,
                "PASSWORD_RESET",
                "USER",
                user.getId(),
                "The user with the id " + user.getId() + " rested password."
        );
    }

    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        auditLogService.log(
                user,
                "PASSWORD_CHANGE",
                "USER",
                user.getId(),
                "The user with the id " + user.getId() + " changed password."
        );
    }


}