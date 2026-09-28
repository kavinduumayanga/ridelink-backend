package com.ridelink.payment.dto;

import com.ridelink.payment.domain.Fare;

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

    public FareResponse(String fareId, String rideId, String fareType, Double distanceKm, Double baseFare, Double ratePerKm, Double totalFare) {
        this.fareId = fareId;
        this.rideId = rideId;
        this.fareType = fareType;
        this.distanceKm = distanceKm;
        this.baseFare = baseFare;
        this.ratePerKm = ratePerKm;
        this.totalFare = totalFare;
    }

    public static FareResponse fromEntity(Fare fare) {
        return new FareResponse(
                fare.getId(),
                fare.getRideId(),
                fare.getFareType() != null ? fare.getFareType().name() : null,
                fare.getDistanceKm(),
                fare.getBaseFare(),
                fare.getRatePerKm(),
                fare.getTotalFare()
        );
    }

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
