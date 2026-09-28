package com.ridelink.ride.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a ride (POST /api/rides).
 * Field names and validation rules match API_CONTRACTS.md §5.2 exactly.
 */
public class CreateRideRequest {

    @NotBlank(message = "passengerId is required")
    private String passengerId;

    @NotBlank(message = "pickupLocation is required")
    private String pickupLocation;

    @NotBlank(message = "dropoffLocation is required")
    private String dropoffLocation;

    @NotNull(message = "pickupLatitude is required")
    @DecimalMin(value = "-90.0", message = "pickupLatitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "pickupLatitude must be <= 90.0")
    private Double pickupLatitude;

    @NotNull(message = "pickupLongitude is required")
    @DecimalMin(value = "-180.0", message = "pickupLongitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "pickupLongitude must be <= 180.0")
    private Double pickupLongitude;

    @NotNull(message = "dropoffLatitude is required")
    @DecimalMin(value = "-90.0", message = "dropoffLatitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "dropoffLatitude must be <= 90.0")
    private Double dropoffLatitude;

    @NotNull(message = "dropoffLongitude is required")
    @DecimalMin(value = "-180.0", message = "dropoffLongitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "dropoffLongitude must be <= 180.0")
    private Double dropoffLongitude;

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be greater than 0")
    private Double distanceKm;

    public CreateRideRequest() {
    }

    // --- Getters and Setters ---

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(String dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public Double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(Double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public Double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(Double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
