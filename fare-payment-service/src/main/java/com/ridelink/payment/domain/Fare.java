package com.ridelink.payment.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

@Document(collection = "fares")
public class Fare {

    @Id
    private String id;

    private String rideId;
    private FareType fareType;
    private Double distanceKm;
    private Double baseFare;
    private Double ratePerKm;
    private Double totalFare;
    private Instant createdAt;

    public Fare() {
    }

    public Fare(String id, String rideId, FareType fareType, Double distanceKm, Double baseFare, Double ratePerKm, Double totalFare, Instant createdAt) {
        this.id = id;
        this.rideId = rideId;
        this.fareType = fareType;
        this.distanceKm = distanceKm;
        this.baseFare = baseFare;
        this.ratePerKm = ratePerKm;
        this.totalFare = totalFare;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public FareType getFareType() {
        return fareType;
    }

    public void setFareType(FareType fareType) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fare fare = (Fare) o;
        return Objects.equals(id, fare.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Fare{" +
                "id='" + id + '\'' +
                ", rideId='" + rideId + '\'' +
                ", fareType=" + fareType +
                ", distanceKm=" + distanceKm +
                ", baseFare=" + baseFare +
                ", ratePerKm=" + ratePerKm +
                ", totalFare=" + totalFare +
                ", createdAt=" + createdAt +
                '}';
    }
}
