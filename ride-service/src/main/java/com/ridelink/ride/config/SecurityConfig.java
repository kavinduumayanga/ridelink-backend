package com.ridelink.ride.config;

import com.ridelink.ride.security.JwtAccessDeniedHandler;
import com.ridelink.ride.security.JwtAuthenticationEntryPoint;
import com.ridelink.ride.security.RideAuthorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationEntryPoint authenticationEntryPoint,
                                            JwtAccessDeniedHandler accessDeniedHandler,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            RideAuthorization rideAuthorization) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rides").hasRole("PASSENGER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/rides/*/accept",
                                "/api/rides/*/start",
                                "/api/rides/*/complete")
                        .access((authentication, context) -> {
                            boolean isDriver = authentication.get().getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_DRIVER"));
                            return new AuthorizationDecision(isDriver && rideAuthorization.isAssignedDriver(
                                    rideIdFromPath(context.getRequest().getRequestURI()),
                                    authentication.get().getName()));
                        })
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/rides/*/assign",
                                "/api/rides/*/cancel")
                        .access((authentication, context) -> {
                            boolean isAdmin = authentication.get().getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
                            boolean isPassenger = authentication.get().getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_PASSENGER"));
                            return new AuthorizationDecision(isAdmin || (isPassenger
                                    && rideAuthorization.isPassengerOwner(
                                    rideIdFromPath(context.getRequest().getRequestURI()),
                                    authentication.get().getName())));
                        })
                        .requestMatchers("/api/rides/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes for HS256");
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> requiredClaimsValidator = jwt -> {
            boolean hasSubject = jwt.getSubject() != null && !jwt.getSubject().isBlank();
            String role = jwt.getClaimAsString("role");
            boolean hasValidRole = role != null
                    && Set.of("PASSENGER", "DRIVER", "ADMIN").contains(role);
            boolean hasIssuedAt = jwt.getIssuedAt() != null;
            boolean hasExpiration = jwt.getExpiresAt() != null;

            if (hasSubject && hasValidRole && hasIssuedAt && hasExpiration) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "JWT is missing required claims",
                    null));
        };

        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                requiredClaimsValidator));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    private static String rideIdFromPath(String requestUri) {
        String[] segments = requestUri.split("/");
        return segments.length > 3 ? segments[3] : "";
    }
}
