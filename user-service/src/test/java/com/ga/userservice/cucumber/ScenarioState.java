package com.ga.userservice.cucumber;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;

@Component
@ScenarioScope
public class ScenarioState {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Response response;

    public RequestSpecification request() {
        return given()
                .contentType("application/json");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}