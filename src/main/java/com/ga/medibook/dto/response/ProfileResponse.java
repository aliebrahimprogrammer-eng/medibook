package com.ga.medibook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {

    private Long userId;
    private String email;
    private String role;

    private String firstName;
    private String lastName;
    private String phone;

    private String profilePictureUrl;
}