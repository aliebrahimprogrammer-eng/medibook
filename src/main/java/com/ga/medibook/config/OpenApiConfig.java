package com.ga.medibook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medibookOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("MediBook API")
                        .version("1.0")
                        .description(
                                "REST API for managing clinic appointments, " +
                                        "doctors, patients, availability, authentication, " +
                                        "and administrative operations."
                        )
                        .contact(new Contact()
                                .name("MediBook Development Team")
                        )
                );
    }
}