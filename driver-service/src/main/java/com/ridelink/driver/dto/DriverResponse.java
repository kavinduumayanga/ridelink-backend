package com.ridelink.driver.dto;

import com.ridelink.driver.domain.DriverAvailability;

public class DriverResponse {

    private String driverId;
    private String accountId;
    private String licenseNumber;
    private String serviceArea;
    private DriverAvailability availability;
    private Double latitude;
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
