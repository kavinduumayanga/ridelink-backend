package com.ridelink.account.dto;

import com.ridelink.account.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User login response with JWT token")
public class LoginResponse {

    @Schema(description = "User ID (MongoDB ObjectId)", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String userId;

    @Schema(description = "User email address", example = "kavindu@example.com")
    private String email;

    @Schema(description = "User role", example = "PASSENGER")
    private Role role;

    @Schema(description = "Issued JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String userId, String email, Role role, String token) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
