package com.ga.businessservice.cucumber;

import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.security.JWTUtils;
import com.ga.businessservice.service.UserServiceClient;
import io.cucumber.java.Before;

import static com.ga.businessservice.cucumber.ScenarioState.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CucumberHooks {

    private final ScenarioState state;
    private final JWTUtils jwtUtils;
    private final UserServiceClient userServiceClient;

    public CucumberHooks(
            ScenarioState state,
            JWTUtils jwtUtils,
            UserServiceClient userServiceClient
    ) {
        this.state = state;
        this.jwtUtils = jwtUtils;
        this.userServiceClient = userServiceClient;
    }

    @Before
    public void setUpScenario() {
        state.reset();

        reset(jwtUtils, userServiceClient);

        when(jwtUtils.validateJwtToken(anyString()))
                .thenReturn(true);

        when(jwtUtils.getUserNameFromJwtToken(anyString()))
                .thenAnswer(invocation -> {
                    String token = invocation.getArgument(0);

                    return switch (token) {
                        case PATIENT_ONE_TOKEN -> PATIENT_ONE_EMAIL;
                        case PATIENT_TWO_TOKEN -> PATIENT_TWO_EMAIL;
                        case DOCTOR_TOKEN -> DOCTOR_EMAIL;
                        default -> throw new IllegalArgumentException(
                                "Unknown test token: " + token
                        );
                    };
                });

        when(userServiceClient.findByEmail(anyString()))
                .thenAnswer(invocation ->
                        userByEmail(invocation.getArgument(0))
                );

        when(userServiceClient.findById(anyLong()))
                .thenAnswer(invocation ->
                        userById(invocation.getArgument(0))
                );
    }

    private InternalUserResponse userByEmail(String email) {
        return switch (email) {
            case PATIENT_ONE_EMAIL ->
                    user(PATIENT_ONE_ID, PATIENT_ONE_EMAIL, "PATIENT",
                            "Patient", "One");

            case PATIENT_TWO_EMAIL ->
                    user(PATIENT_TWO_ID, PATIENT_TWO_EMAIL, "PATIENT",
                            "Patient", "Two");

            case DOCTOR_EMAIL ->
                    user(DOCTOR_USER_ID, DOCTOR_EMAIL, "DOCTOR",
                            "Doctor", "One");

            default -> null;
        };
    }

    private InternalUserResponse userById(Long id) {
        if (id == null) {
            return null;
        }

        if (id.equals(PATIENT_ONE_ID)) {
            return user(PATIENT_ONE_ID, PATIENT_ONE_EMAIL, "PATIENT",
                    "Patient", "One");
        }

        if (id.equals(PATIENT_TWO_ID)) {
            return user(PATIENT_TWO_ID, PATIENT_TWO_EMAIL, "PATIENT",
                    "Patient", "Two");
        }

        if (id.equals(DOCTOR_USER_ID)) {
            return user(DOCTOR_USER_ID, DOCTOR_EMAIL, "DOCTOR",
                    "Doctor", "One");
        }

        return null;
    }

    private InternalUserResponse user(
            Long id,
            String email,
            String role,
            String firstName,
            String lastName
    ) {
        return new InternalUserResponse(
                id,
                email,
                role,
                "ACTIVE",
                true,
                firstName,
                lastName
        );
    }
}


