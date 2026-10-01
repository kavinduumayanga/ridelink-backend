package com.ridelink.account.dto;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Account user profile response")
public class UserResponse {

    @Schema(description = "User ID (MongoDB ObjectId)", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String userId;

    @Schema(description = "User's first name", example = "Kavindu")
    private String firstName;

    @Schema(description = "User's last name", example = "Umayanga")
    private String lastName;

    @Schema(description = "User's email address", example = "kavindu@example.com")
    private String email;

    @Schema(description = "User's phone number", example = "+94771234567")
    private String phone;

    @Schema(description = "Account role", example = "PASSENGER")
    private Role role;

    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus status;

    public UserResponse() {
    }

    public UserResponse(String userId, String firstName, String lastName, String email, String phone, Role role, AccountStatus status) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
