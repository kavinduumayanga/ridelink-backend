package com.ridelink.payment.dto;

import com.ridelink.payment.domain.Fare;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO representing estimated or final calculated fare")
public class FareResponse {

    @Schema(description = "MongoDB fare ID", example = "aa5d4e5f6a7b8c9d0e1f2a3b")
    private String fareId;

    @Schema(description = "MongoDB ride ID", example = "995c3d4e5f6a7b8c9d0e1f2a")
    private String rideId;

    @Schema(description = "Type of fare calculation: ESTIMATE or FINAL", example = "ESTIMATE", allowableValues = {"ESTIMATE", "FINAL"})
    private String fareType;

    @Schema(description = "Distance in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Base fare amount in LKR", example = "200.00")
    private Double baseFare;

    @Schema(description = "Rate per kilometer in LKR", example = "50.00")
    private Double ratePerKm;

    @Schema(description = "Total calculated fare amount in LKR", example = "825.00")
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
