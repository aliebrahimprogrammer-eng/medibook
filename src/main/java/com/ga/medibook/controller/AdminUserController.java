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

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

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

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> findAllUsers() {

        return ResponseEntity.ok(
                adminUserService.findAllUsers()
        );
    }

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