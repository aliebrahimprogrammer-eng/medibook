package com.ga.medibook.service;

import com.ga.medibook.dto.response.AdminUserResponse;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional
    public AdminUserResponse updateUserRole(
            Long userId,
            String adminEmail,
            UserRole newRole
    ) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can change user roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (admin.getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        if (user.getRole() == newRole) {
            throw new IllegalArgumentException("User already has this role");
        }

        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {

            long adminCount = userRepository.countByRole(UserRole.ADMIN);

            if (adminCount <= 1) {
                throw new IllegalArgumentException(
                        "The last admin cannot be demoted"
                );
            }
        }

        UserRole oldRole = user.getRole();

        user.setRole(newRole);
        userRepository.save(user);

        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException(
                    "User is already inactive"
            );
        }

        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
    }

    @Transactional
    public void reactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "User is already active"
            );
        }

        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
    }

    private AdminUserResponse toResponse(User user) {

        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified()
        );
    }
}