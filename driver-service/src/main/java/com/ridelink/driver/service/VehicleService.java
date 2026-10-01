package com.ridelink.driver.service;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.Vehicle;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public VehicleResponse createVehicle(String driverId, VehicleRequest request) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found with id: " + driverId);
        }

        validateDriverOwnership(driverId);

        if (vehicleRepository.existsByDriverId(driverId)) {
            throw new DuplicateResourceException("Vehicle already registered for driver id: " + driverId);
        }

        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DuplicateResourceException("Vehicle with license plate already exists: " + request.getLicensePlate());
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setDriverId(driverId);
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setColor(request.getColor());
        vehicle.setVehicleType(request.getVehicleType());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return mapToVehicleResponse(savedVehicle);
    }

    public VehicleResponse updateVehicle(String driverId, VehicleRequest request) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found with id: " + driverId);
        }

        validateDriverOwnership(driverId);

        Vehicle vehicle = vehicleRepository.findByDriverId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for driver id: " + driverId));

        if (!vehicle.getLicensePlate().equalsIgnoreCase(request.getLicensePlate()) &&
                vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DuplicateResourceException("Vehicle with license plate already exists: " + request.getLicensePlate());
        }

        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setColor(request.getColor());
        vehicle.setVehicleType(request.getVehicleType());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToVehicleResponse(updatedVehicle);
    }

    private void validateDriverOwnership(String driverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver != null) {
            String currentUserId = auth.getName();
            if (driver.getAccountId() != null && !driver.getAccountId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied: You do not own this driver resource");
            }
        }
    }

    private VehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getVehicleId(),
                vehicle.getDriverId(),
                vehicle.getLicensePlate(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getVehicleType()
        );
    }
}
