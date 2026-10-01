package com.ridelink.ride.dto;

/**
 * Interservice DTO to consume the eligible-driver response from Driver Service.
 * Fields match the Driver Service response shape defined in API_CONTRACTS.md §3.7.
 *
 * Only the fields needed by Ride Service for driver selection are included.
 * No driver profile information, vehicle details, or persistence entities are imported.
 */
public class AvailableDriverResponse {

    private String driverId;
    private String accountId;
    private String licenseNumber;
    private String serviceArea;
    private String availability;
    private Double latitude;
    private Double longitude;

    public AvailableDriverResponse() {
    }

    // --- Getters and Setters ---

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

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
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
