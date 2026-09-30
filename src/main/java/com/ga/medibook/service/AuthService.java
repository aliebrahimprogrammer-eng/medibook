package com.ga.medibook.service;

import com.ga.medibook.dto.request.LoginRequest;
import com.ga.medibook.dto.request.RegisterRequest;
import com.ga.medibook.dto.response.LoginResponse;
import com.ga.medibook.dto.response.UserResponse;
import com.ga.medibook.model.entity.EmailVerificationToken;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.EmailVerificationTokenRepository;
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

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
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
    }


}