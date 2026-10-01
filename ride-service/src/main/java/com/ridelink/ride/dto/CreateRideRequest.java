package com.ridelink.ride.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for creating a ride (POST /api/rides).
 * Field names and validation rules match API_CONTRACTS.md §5.2 exactly.
 */
@Schema(description = "Fields required to request a ride")
public class CreateRideRequest {

    @NotBlank(message = "passengerId is required")
    @Schema(description = "Account Service user ID; must equal JWT subject", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String passengerId;

    @NotBlank(message = "pickupLocation is required")
    @Schema(example = "Colombo Fort")
    private String pickupLocation;

    @NotBlank(message = "dropoffLocation is required")
    @Schema(example = "Bambalapitiya")
    private String dropoffLocation;

    @NotNull(message = "pickupLatitude is required")
    @DecimalMin(value = "-90.0", message = "pickupLatitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "pickupLatitude must be <= 90.0")
    @Schema(example = "6.9340", minimum = "-90", maximum = "90")
    private Double pickupLatitude;

    @NotNull(message = "pickupLongitude is required")
    @DecimalMin(value = "-180.0", message = "pickupLongitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "pickupLongitude must be <= 180.0")
    @Schema(example = "79.8428", minimum = "-180", maximum = "180")
    private Double pickupLongitude;

    @NotNull(message = "dropoffLatitude is required")
    @DecimalMin(value = "-90.0", message = "dropoffLatitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "dropoffLatitude must be <= 90.0")
    @Schema(example = "6.8942", minimum = "-90", maximum = "90")
    private Double dropoffLatitude;

    @NotNull(message = "dropoffLongitude is required")
    @DecimalMin(value = "-180.0", message = "dropoffLongitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "dropoffLongitude must be <= 180.0")
    @Schema(example = "79.8558", minimum = "-180", maximum = "180")
    private Double dropoffLongitude;

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be greater than 0")
    @Schema(example = "5.5", exclusiveMinimum = true, minimum = "0")
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
