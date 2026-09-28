package com.ridelink.driver.dto;

import com.ridelink.driver.domain.VehicleType;

public class VehicleResponse {

    private String vehicleId;
    private String driverId;
    private String licensePlate;
    private String make;
    private String model;
    private Integer year;
    private String color;
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
