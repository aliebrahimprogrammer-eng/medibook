package com.ga.medibook.service;

import com.ga.medibook.dto.request.RegisterRequest;
import com.ga.medibook.dto.response.UserResponse;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

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
}