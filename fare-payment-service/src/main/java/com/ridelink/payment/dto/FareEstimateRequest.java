package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FareEstimateRequest {

    @NotBlank(message = "rideId is required")
    private String rideId;

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be greater than 0")
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
