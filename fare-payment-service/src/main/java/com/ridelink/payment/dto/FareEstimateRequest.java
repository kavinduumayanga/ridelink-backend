package com.ridelink.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for estimating ride fare")
public class FareEstimateRequest {

    @NotBlank(message = "rideId is required")
    @Schema(description = "MongoDB ride ID", example = "995c3d4e5f6a7b8c9d0e1f2a", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rideId;

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be greater than 0")
    @Schema(description = "Estimated ride distance in kilometers (must be > 0)", example = "12.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double distanceKm;

    public FareEstimateRequest() {
    }

    public FareEstimateRequest(String rideId, Double distanceKm) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
