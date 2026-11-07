
package com.ga.userservice.service;

import com.ga.userservice.dto.response.InternalUserResponse;
import com.ga.userservice.model.entity.User;
import com.ga.userservice.model.entity.UserProfile;
import com.ga.userservice.repository.UserProfileRepository;
import com.ga.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public InternalUserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public InternalUserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return toResponse(user);
    }

    private InternalUserResponse toResponse(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("User profile not found"));

        return new InternalUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                profile.getFirstName(),
                profile.getLastName()
        );
    }
}
