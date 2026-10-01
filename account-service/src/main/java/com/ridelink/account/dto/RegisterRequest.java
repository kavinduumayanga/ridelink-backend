package com.ridelink.account.dto;

import com.ridelink.account.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration payload")
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "Kavindu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Umayanga", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address (unique login identifier)", example = "kavindu@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "User's phone number", example = "+94771234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password (min 8 characters)", example = "secureP@ss1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "Role is required")
    @Schema(description = "Role for the account (PASSENGER or DRIVER)", example = "PASSENGER", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    public RegisterRequest() {
    }

    public RegisterRequest(String firstName, String lastName, String email, String phone, String password, Role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
