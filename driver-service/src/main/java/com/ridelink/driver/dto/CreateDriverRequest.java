package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a driver operational profile")
public class CreateDriverRequest {

    @NotBlank(message = "accountId is required")
    @Schema(description = "Account Service userId — must match the JWT sub claim", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String accountId;

    @NotBlank(message = "licenseNumber is required")
    @Schema(description = "Driver's license number", example = "DL-123456")
    private String licenseNumber;

    @NotBlank(message = "serviceArea is required")
    @Schema(description = "City or zone name the driver operates in", example = "Colombo")
    private String serviceArea;

    public CreateDriverRequest() {
    }

    public CreateDriverRequest(String accountId, String licenseNumber, String serviceArea) {
        this.accountId = accountId;
        this.licenseNumber = licenseNumber;
        this.serviceArea = serviceArea;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }
}
