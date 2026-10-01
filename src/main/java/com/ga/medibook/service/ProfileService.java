package com.ga.medibook.service;

import com.ga.medibook.dto.request.ProfileUpdateRequest;
import com.ga.medibook.dto.response.ProfileResponse;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        UserProfile profile =
                userProfileRepository.findByUserId(
                        user.getId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Profile not found"
                        )
                );

        return toResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateMyProfile(
            String email,
            ProfileUpdateRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        UserProfile profile =
                userProfileRepository.findByUserId(
                        user.getId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Profile not found"
                        )
                );

        profile.setFirstName(
                request.getFirstName()
        );

        profile.setLastName(
                request.getLastName()
        );

        profile.setPhone(
                request.getPhone()
        );

        UserProfile saved =
                userProfileRepository.save(profile);

        return toResponse(user, saved);
    }

    private ProfileResponse toResponse(
            User user,
            UserProfile profile
    ) {

        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getProfilePictureUrl()
        );
    }
}