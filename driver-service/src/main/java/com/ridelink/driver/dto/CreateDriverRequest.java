package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDriverRequest {

    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotBlank(message = "licenseNumber is required")
    private String licenseNumber;

    @NotBlank(message = "serviceArea is required")
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
