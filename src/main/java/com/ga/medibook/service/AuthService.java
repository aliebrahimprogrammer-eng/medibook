package com.ga.medibook.service;

import com.ga.medibook.dto.request.*;
import com.ga.medibook.dto.response.LoginResponse;
import com.ga.medibook.dto.response.UserResponse;
import com.ga.medibook.exception.ResourceConflictException;
import com.ga.medibook.model.entity.EmailVerificationToken;
import com.ga.medibook.model.entity.PasswordResetToken;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.EmailVerificationTokenRepository;
import com.ga.medibook.repository.PasswordResetTokenRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import com.ga.medibook.security.JWTUtils;
import com.ga.medibook.security.MyUserDetails;
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