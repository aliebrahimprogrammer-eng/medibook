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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

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

        auditLogService.log(
                user,
                "UPDATE_PROFILE",
                "USER_PROFILE",
                profile.getId(),
                "Profile id " + profile.getId() + " is updated by user id " +  user.getId()
        );

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

    @Transactional
    public ProfileResponse uploadProfilePicture(
            String email,
            MultipartFile file
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

        String filename =
                fileStorageService.storeProfilePicture(file);

        profile.setProfilePictureUrl(filename);

        UserProfile saved =
                userProfileRepository.save(profile);

        auditLogService.log(
                user,
                "UPLOAD_PROFILE_PICTURE",
                "USER_PROFILE",
                profile.getId(),
                "Profile picture for user profile id " + profile.getId() + " is uploaded by user id " +  user.getId()
        );

        return toResponse(user, saved);
    }
}