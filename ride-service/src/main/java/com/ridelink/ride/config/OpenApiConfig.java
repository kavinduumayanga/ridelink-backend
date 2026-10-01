package com.ridelink.ride.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "RideLink Ride Management API",
        version = "1.0",
        description = "Creates, retrieves, assigns, and manages the lifecycle of rides."))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT issued by RideLink Account Service")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
