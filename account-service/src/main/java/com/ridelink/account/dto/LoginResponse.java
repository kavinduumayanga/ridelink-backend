package com.ridelink.account.dto;

import com.ridelink.account.domain.Role;

public class LoginResponse {

    private String userId;
    private String email;
    private Role role;
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
