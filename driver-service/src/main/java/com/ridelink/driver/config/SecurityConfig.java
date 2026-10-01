package com.ridelink.driver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.ErrorResponse;
import com.ridelink.driver.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Swagger/OpenAPI endpoints — public
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 3.1 POST /api/drivers — Create Driver Profile (DRIVER role, own accountId)
                        .requestMatchers(HttpMethod.POST, "/api/drivers").hasRole("DRIVER")

                        // 3.7 GET /api/drivers/available — Get Available Drivers (any authenticated)
                        .requestMatchers(HttpMethod.GET, "/api/drivers/available").authenticated()

                        // 3.2 GET /api/drivers/{driverId} — Get Driver (any authenticated)
                        .requestMatchers(HttpMethod.GET, "/api/drivers/*").authenticated()

                        // 3.3 POST /api/drivers/{driverId}/vehicle — Create Vehicle (DRIVER role)
                        .requestMatchers(HttpMethod.POST, "/api/drivers/*/vehicle").hasRole("DRIVER")

                        // 3.4 PUT /api/drivers/{driverId}/vehicle — Update Vehicle (DRIVER role)
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/*/vehicle").hasRole("DRIVER")

                        // 3.5 PATCH /api/drivers/{driverId}/availability — Update Availability (DRIVER role)
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/availability").hasRole("DRIVER")

                        // 3.6 PATCH /api/drivers/{driverId}/location — Update Location (DRIVER role)
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/location").hasRole("DRIVER")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        // 401 — unauthenticated (no token or invalid token)
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorResponse errorResponse = new ErrorResponse(
                                    Instant.now(),
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHENTICATED",
                                    "Missing or invalid JWT",
                                    request.getRequestURI()
                            );
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        })
                        // 403 — forbidden (valid JWT but insufficient role)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ErrorResponse errorResponse = new ErrorResponse(
                                    Instant.now(),
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN",
                                    "Valid JWT but insufficient role",
                                    request.getRequestURI()
                            );
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
