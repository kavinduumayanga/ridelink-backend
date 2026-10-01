package com.ridelink.driver.dto;

import com.ridelink.driver.domain.DriverAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Driver operational profile response")
public class DriverResponse {

    @Schema(description = "MongoDB ObjectId of the driver profile", example = "775a1b2c3d4e5f6a7b8c9d0e")
    private String driverId;

    @Schema(description = "Account Service userId that owns this driver profile", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String accountId;

    @Schema(description = "Driver's license number", example = "DL-123456")
    private String licenseNumber;

    @Schema(description = "City or zone name the driver operates in", example = "Colombo")
    private String serviceArea;

    @Schema(description = "Current availability status", example = "AVAILABLE")
    private DriverAvailability availability;

    @Schema(description = "Simulated latitude coordinate", example = "6.9271")
    private Double latitude;

    @Schema(description = "Simulated longitude coordinate", example = "79.8612")
    private Double longitude;

    public DriverResponse() {
    }

    public DriverResponse(String driverId, String accountId, String licenseNumber, String serviceArea,
                          DriverAvailability availability, Double latitude, Double longitude) {
        this.driverId = driverId;
        this.accountId = accountId;
        this.licenseNumber = licenseNumber;
        this.serviceArea = serviceArea;
        this.availability = availability;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
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

    public DriverAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(DriverAvailability availability) {
        this.availability = availability;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
