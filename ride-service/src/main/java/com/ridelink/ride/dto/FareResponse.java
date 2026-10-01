package com.ridelink.ride.dto;

/**
 * Response DTO to consume the Fare Service response.
 * Fields match API_CONTRACTS.md §4.1 and §4.2 response shapes exactly.
 *
 * Ride Service extracts totalFare and stores it as:
 * - estimatedFare (from estimate endpoint, §6.2)
 * - finalFare (from final endpoint, §6.3)
 *
 * No Fare Service persistence entities are imported.
 */
public class FareResponse {

    private String fareId;
    private String rideId;
    private String fareType;
    private Double distanceKm;
    private Double baseFare;
    private Double ratePerKm;
    private Double totalFare;

    public FareResponse() {
    }

    // --- Getters and Setters ---

    public String getFareId() {
        return fareId;
    }

    public void setFareId(String fareId) {
        this.fareId = fareId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(Double baseFare) {
        this.baseFare = baseFare;
    }

    public Double getRatePerKm() {
        return ratePerKm;
    }

    public void setRatePerKm(Double ratePerKm) {
        this.ratePerKm = ratePerKm;
    }

    public Double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(Double totalFare) {
        this.totalFare = totalFare;
    }
}
