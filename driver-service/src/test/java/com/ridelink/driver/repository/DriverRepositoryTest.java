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

    @Test
    public void testUpdateAvailability_Persisted() {
        Driver driver = new Driver();
        driver.setAccountId("account-avail-1");
        driver.setLicenseNumber("DL-AVAIL-1");
        driver.setServiceArea("Colombo");
        driver.setAvailability(DriverAvailability.UNAVAILABLE);
        driver.setLatitude(6.9271);
        driver.setLongitude(79.8612);

        Driver savedDriver = driverRepository.save(driver);
        assertNotNull(savedDriver.getDriverId());
        assertEquals(DriverAvailability.UNAVAILABLE, savedDriver.getAvailability());

        savedDriver.setAvailability(DriverAvailability.AVAILABLE);
        Driver updatedDriver = driverRepository.save(savedDriver);
        assertEquals(DriverAvailability.AVAILABLE, updatedDriver.getAvailability());

        Optional<Driver> reloaded = driverRepository.findById(savedDriver.getDriverId());
        assertTrue(reloaded.isPresent());
        assertEquals(DriverAvailability.AVAILABLE, reloaded.get().getAvailability());
        assertEquals("Colombo", reloaded.get().getServiceArea());

        // Cleanup
        driverRepository.delete(updatedDriver);
    }

    @Test
    public void testUpdateLocation_Persisted() {
        Driver driver = new Driver();
        driver.setAccountId("account-loc-1");
        driver.setLicenseNumber("DL-LOC-1");
        driver.setServiceArea("Kandy");
        driver.setAvailability(DriverAvailability.AVAILABLE);
        driver.setLatitude(0.0);
        driver.setLongitude(0.0);

        Driver savedDriver = driverRepository.save(driver);
        assertNotNull(savedDriver.getDriverId());

        savedDriver.setLatitude(7.2906);
        savedDriver.setLongitude(80.6337);
        Driver updatedDriver = driverRepository.save(savedDriver);
        assertEquals(7.2906, updatedDriver.getLatitude());
        assertEquals(80.6337, updatedDriver.getLongitude());

        Optional<Driver> reloaded = driverRepository.findById(savedDriver.getDriverId());
        assertTrue(reloaded.isPresent());
        assertEquals(7.2906, reloaded.get().getLatitude());
        assertEquals(80.6337, reloaded.get().getLongitude());
        assertEquals("Kandy", reloaded.get().getServiceArea());

        // Cleanup
        driverRepository.delete(updatedDriver);
    }
}
