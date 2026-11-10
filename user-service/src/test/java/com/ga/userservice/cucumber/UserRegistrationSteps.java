package com.ga.userservice.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor
public class UserRegistrationSteps {

    private final ScenarioState state;

    @Given("I have valid registration details")
    public void iHaveValidRegistrationDetails() {

        state.setEmail(
                "cucumber-" + UUID.randomUUID() + "@example.com"
        );

        state.setPassword("SecurePass123!");
        state.setFirstName("Cucumber");
        state.setLastName("Tester");
    }

    @When("I submit the registration request")
    public void iSubmitTheRegistrationRequest() {

        state.setResponse(
                state.request()
                        .body("""
                                {
                                    "email": "%s",
                                    "password": "%s",
                                    "firstName": "%s",
                                    "lastName": "%s",
                                    "phone": "+97330000000"
                                }
                                """.formatted(
                                state.getEmail(),
                                state.getPassword(),
                                state.getFirstName(),
                                state.getLastName()
                        ))
                        .when()
                        .post("/api/auth/register")
        );
    }

    @Then("the registration should be successful")
    public void theRegistrationShouldBeSuccessful() {

        assertEquals(
                201,
                state.getResponse().getStatusCode(),
                "Registration should return HTTP 201 Created"
        );
    }

    @Then("the response should contain the registered user's details")
    public void theResponseShouldContainRegisteredUserDetails() {

        Response response = state.getResponse();

        assertNotNull(response.jsonPath().getLong("id"));
        assertEquals(
                state.getEmail(),
                response.jsonPath().getString("email")
        );
        assertEquals(
                "PATIENT",
                response.jsonPath().getString("role")
        );
        assertEquals(
                "ACTIVE",
                response.jsonPath().getString("status")
        );
        assertFalse(
                response.jsonPath().getBoolean("emailVerified")
        );
    }
}