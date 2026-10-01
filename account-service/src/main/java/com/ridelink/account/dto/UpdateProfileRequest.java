package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Profile update request payload")
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Schema(description = "User's updated first name", example = "Kavindu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's updated last name", example = "Umayanga", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "User's updated phone number", example = "+94779999999", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
