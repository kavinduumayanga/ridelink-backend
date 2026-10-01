package com.ridelink.driver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.ErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Extracts and validates the JWT from the Authorization header on every request.
 * Sets the SecurityContext with the userId as principal and role as authority.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token present — let Spring Security handle the 401
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        UsernamePasswordAuthenticationToken authentication;
        try {
            Claims claims = jwtTokenProvider.validateAndGetClaims(token);
            String userId = jwtTokenProvider.getUserId(claims);
            String role = jwtTokenProvider.getRole(claims);

            if (userId == null || userId.trim().isEmpty() || role == null || role.trim().isEmpty()) {
                writeErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "Missing required JWT claims");
                return;
            }

            if (!"PASSENGER".equals(role) && !"DRIVER".equals(role) && !"ADMIN".equals(role)) {
                writeErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "Invalid role claim in JWT");
                return;
            }

            // Set the authentication with ROLE_ prefix for Spring Security
            authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

        } catch (JwtException e) {
            writeErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "Invalid or expired JWT token");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpServletRequest request,
                                    HttpStatus status, String error, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
