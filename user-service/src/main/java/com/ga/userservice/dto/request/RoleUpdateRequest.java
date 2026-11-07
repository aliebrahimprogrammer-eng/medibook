package com.ga.userservice.dto.request;

import com.ga.userservice.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}