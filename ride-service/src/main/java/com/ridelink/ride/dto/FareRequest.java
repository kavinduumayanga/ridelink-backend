package com.ridelink.ride.dto;

/**
 * Request DTO for Fare Service estimate and final fare endpoints.
 * Fields match API_CONTRACTS.md §4.1 and §4.2 request shapes exactly.
 *
 * Used by Ride Service when calling:
 * - POST /api/fares/estimate (§6.2)
 * - POST /api/fares/final (§6.3)
 */
public class FareRequest {

    private String rideId;
    private Double distanceKm;

    public FareRequest() {
    }

    public FareRequest(String rideId, Double distanceKm) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
    }

    // --- Getters and Setters ---

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
