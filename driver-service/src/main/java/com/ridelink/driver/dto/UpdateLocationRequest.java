package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update driver simulated GPS location")
public class UpdateLocationRequest {

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "latitude must be between -90.0 and 90.0")
    @Schema(description = "Latitude coordinate (-90.0 to 90.0)", example = "6.9271")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "longitude must be between -180.0 and 180.0")
    @Schema(description = "Longitude coordinate (-180.0 to 180.0)", example = "79.8612")
    private Double longitude;

    public UpdateLocationRequest() {
    }

    public UpdateLocationRequest(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
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
