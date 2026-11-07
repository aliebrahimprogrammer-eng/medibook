package com.ga.businessservice.service;

import com.ga.businessservice.dto.response.InternalUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public UserServiceClient(
            @Value("${USER_SERVICE_BASE_URL:http://localhost:8081}") String baseUrl,
            @Value("${USER_SERVICE_INTERNAL_API_KEY}") String internalApiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.internalApiKey = internalApiKey;
    }

    public InternalUserResponse findById(Long id) {
        return restClient.get()
                .uri("/internal/users/{id}", id)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(InternalUserResponse.class);
    }

    public InternalUserResponse findByEmail(String email) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/users/by-email")
                        .queryParam("email", email)
                        .build())
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(InternalUserResponse.class);
    }
}