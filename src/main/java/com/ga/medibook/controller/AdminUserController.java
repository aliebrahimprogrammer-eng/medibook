package com.ga.medibook.controller;

import com.ga.medibook.dto.request.RoleUpdateRequest;
import com.ga.medibook.dto.response.AdminUserResponse;
import com.ga.medibook.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "Update user role",
            description = "Updates the role of an existing user"
    )
    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            Authentication authentication
    ) {
        AdminUserResponse response = adminUserService.updateUserRole(
                id,
                authentication.getName(),
                request.getRole()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users"
    )
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> findAllUsers() {

        return ResponseEntity.ok(
                adminUserService.findAllUsers()
        );
    }

    @Operation(
            summary = "Deactivate user",
            description = "Deactivates an existing user account"
    )
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            Authentication authentication,
            @PathVariable Long id
    ){

        adminUserService.deactivateUser(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reactivate user",
            description = "Reactivates a previously deactivated user account"
    )
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(
            Authentication authentication,
            @PathVariable Long id
    ) {

        adminUserService.reactivateUser(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

}