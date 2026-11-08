
package com.ga.userservice.dto.response;

import com.ga.userservice.model.enums.UserRole;
import com.ga.userservice.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternalUserResponse {

    private Long id;
    private String email;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private String firstName;
    private String lastName;
}
