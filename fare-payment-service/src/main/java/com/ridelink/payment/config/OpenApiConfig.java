package com.ridelink.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI farePaymentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink Fare & Payment Service API")
                        .description("Microservice responsible for fare calculation, simulated payments, and receipt generation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RideLink Team")
                                .url("https://github.com/kavinduumayanga/ridelink-backend")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token issued by Account Service (e.g. Bearer <token>)")));
    }
}
