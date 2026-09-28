package com.ridelink.driver.repository;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
public class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    @Test
    public void testSaveAndFindByAvailabilityAndServiceArea() {
        Driver driver = new Driver();
        driver.setAccountId("account-123");
        driver.setLicenseNumber("DL-12345");
        driver.setServiceArea("Colombo");
        driver.setAvailability(DriverAvailability.AVAILABLE);
        driver.setLatitude(6.9271);
        driver.setLongitude(79.8612);

        Driver savedDriver = driverRepository.save(driver);
        assertNotNull(savedDriver.getDriverId());

        List<Driver> availableDrivers = driverRepository.findByAvailabilityAndServiceArea(DriverAvailability.AVAILABLE, "Colombo");
        assertFalse(availableDrivers.isEmpty());
        assertEquals("Colombo", availableDrivers.get(0).getServiceArea());

        // Cleanup
        driverRepository.delete(savedDriver);
    }
}
