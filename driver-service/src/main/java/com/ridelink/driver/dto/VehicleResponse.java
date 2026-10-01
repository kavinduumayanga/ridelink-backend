package com.ridelink.driver.dto;

import com.ridelink.driver.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle details response")
public class VehicleResponse {

    @Schema(description = "Unique vehicle identifier", example = "660c149b14b8a25c12345678")
    private String vehicleId;

    @Schema(description = "Associated driver ID", example = "660c149b14b8a25c87654321")
    private String driverId;

    @Schema(description = "Vehicle license plate number", example = "CAB-1234")
    private String licensePlate;

    @Schema(description = "Vehicle manufacturer/make", example = "Toyota")
    private String make;

    @Schema(description = "Vehicle model", example = "Prius")
    private String model;

    @Schema(description = "Manufacturing year", example = "2020")
    private Integer year;

    @Schema(description = "Vehicle color", example = "White")
    private String color;

    @Schema(description = "Type of vehicle", example = "CAR")
    private VehicleType vehicleType;

    public VehicleResponse() {
    }

    public VehicleResponse(String vehicleId, String driverId, String licensePlate, String make,
                           String model, Integer year, String color, VehicleType vehicleType) {
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.vehicleType = vehicleType;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }
}
