package com.ridelink.driver.controller;

import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final VehicleService vehicleService;

    public DriverController(DriverService driverService, VehicleService vehicleService) {
        this.driverService = driverService;
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @RequestParam(required = false) String serviceArea) {
        List<DriverResponse> response = driverService.getAvailableDrivers(serviceArea);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String driverId) {
        DriverResponse response = driverService.getDriverById(driverId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{driverId}/vehicle")
    public ResponseEntity<VehicleResponse> createVehicle(
            @PathVariable String driverId,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(driverId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{driverId}/vehicle")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable String driverId,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(driverId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        DriverResponse response = driverService.updateAvailability(driverId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateLocationRequest request) {
        DriverResponse response = driverService.updateLocation(driverId, request);
        return ResponseEntity.ok(response);
    }
}
