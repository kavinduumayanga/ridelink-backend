package com.ridelink.driver.dto;

import com.ridelink.driver.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for creating or updating a vehicle")
public class VehicleRequest {

    @NotBlank(message = "licensePlate is required")
    @Schema(description = "Vehicle license plate number", example = "CAB-1234")
    private String licensePlate;

    @NotBlank(message = "make is required")
    @Schema(description = "Vehicle manufacturer/make", example = "Toyota")
    private String make;

    @NotBlank(message = "model is required")
    @Schema(description = "Vehicle model", example = "Prius")
    private String model;

    @NotNull(message = "year is required")
    @Positive(message = "year must be positive")
    @Schema(description = "Manufacturing year", example = "2020")
    private Integer year;

    @NotBlank(message = "color is required")
    @Schema(description = "Vehicle color", example = "White")
    private String color;

    @NotNull(message = "vehicleType is required")
    @Schema(description = "Type of vehicle", example = "CAR")
    private VehicleType vehicleType;

    public VehicleRequest() {
    }

    public VehicleRequest(String licensePlate, String make, String model, Integer year, String color, VehicleType vehicleType) {
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.vehicleType = vehicleType;
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
