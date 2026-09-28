package com.ridelink.driver.service;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverRepository;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse createDriver(CreateDriverRequest request) {
        if (driverRepository.existsByAccountId(request.getAccountId())) {
            throw new DuplicateResourceException("Driver profile already exists for accountId: " + request.getAccountId());
        }

        Driver driver = new Driver();
        driver.setAccountId(request.getAccountId());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setServiceArea(request.getServiceArea());
        driver.setAvailability(DriverAvailability.UNAVAILABLE);
        driver.setLatitude(0.0);
        driver.setLongitude(0.0);

        Driver savedDriver = driverRepository.save(driver);
        return mapToDriverResponse(savedDriver);
    }

    public DriverResponse getDriverById(String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        return mapToDriverResponse(driver);
    }

    private DriverResponse mapToDriverResponse(Driver driver) {
        return new DriverResponse(
                driver.getDriverId(),
                driver.getAccountId(),
                driver.getLicenseNumber(),
                driver.getServiceArea(),
                driver.getAvailability(),
                driver.getLatitude(),
                driver.getLongitude()
        );
    }
}
