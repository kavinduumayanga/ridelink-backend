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

    @Test
    public void testFindByAvailabilityOrderByDriverIdAsc() {
        Driver d1 = new Driver();
        d1.setAccountId("acc-avail-order-1");
        d1.setLicenseNumber("DL-ORD-1");
        d1.setServiceArea("Colombo");
        d1.setAvailability(DriverAvailability.AVAILABLE);

        Driver d2 = new Driver();
        d2.setAccountId("acc-avail-order-2");
        d2.setLicenseNumber("DL-ORD-2");
        d2.setServiceArea("Kandy");
        d2.setAvailability(DriverAvailability.AVAILABLE);

        Driver d3 = new Driver();
        d3.setAccountId("acc-avail-order-3");
        d3.setLicenseNumber("DL-ORD-3");
        d3.setServiceArea("Colombo");
        d3.setAvailability(DriverAvailability.UNAVAILABLE);

        Driver saved1 = driverRepository.save(d1);
        Driver saved2 = driverRepository.save(d2);
        Driver saved3 = driverRepository.save(d3);

        List<Driver> available = driverRepository.findByAvailabilityOrderByDriverIdAsc(DriverAvailability.AVAILABLE);
        assertFalse(available.isEmpty());

        // All returned drivers must be AVAILABLE
        assertTrue(available.stream().allMatch(d -> d.getAvailability() == DriverAvailability.AVAILABLE));

        // UNAVAILABLE driver must never appear
        assertFalse(available.stream().anyMatch(d -> d.getDriverId().equals(saved3.getDriverId())));

        // Results must be ordered by driverId ascending
        for (int i = 0; i < available.size() - 1; i++) {
            assertTrue(available.get(i).getDriverId().compareTo(available.get(i + 1).getDriverId()) <= 0);
        }

        // Cleanup
        driverRepository.delete(saved1);
        driverRepository.delete(saved2);
        driverRepository.delete(saved3);
    }

    @Test
    public void testFindByAvailabilityAndServiceAreaOrderByDriverIdAsc() {
        Driver d1 = new Driver();
        d1.setAccountId("acc-area-order-1");
        d1.setLicenseNumber("DL-AREA-1");
        d1.setServiceArea("Galle");
        d1.setAvailability(DriverAvailability.AVAILABLE);

        Driver d2 = new Driver();
        d2.setAccountId("acc-area-order-2");
        d2.setLicenseNumber("DL-AREA-2");
        d2.setServiceArea("Galle");
        d2.setAvailability(DriverAvailability.AVAILABLE);

        Driver d3 = new Driver();
        d3.setAccountId("acc-area-order-3");
        d3.setLicenseNumber("DL-AREA-3");
        d3.setServiceArea("Colombo");
        d3.setAvailability(DriverAvailability.AVAILABLE);

        Driver d4 = new Driver();
        d4.setAccountId("acc-area-order-4");
        d4.setLicenseNumber("DL-AREA-4");
        d4.setServiceArea("Galle");
        d4.setAvailability(DriverAvailability.UNAVAILABLE);

        Driver saved1 = driverRepository.save(d1);
        Driver saved2 = driverRepository.save(d2);
        Driver saved3 = driverRepository.save(d3);
        Driver saved4 = driverRepository.save(d4);

        List<Driver> result = driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(
                DriverAvailability.AVAILABLE, "Galle");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(d -> d.getAvailability() == DriverAvailability.AVAILABLE));
        assertTrue(result.stream().allMatch(d -> "Galle".equals(d.getServiceArea())));
        assertTrue(result.get(0).getDriverId().compareTo(result.get(1).getDriverId()) <= 0);

        // Excludes different service area
        assertFalse(result.stream().anyMatch(d -> d.getDriverId().equals(saved3.getDriverId())));
        // Excludes UNAVAILABLE
        assertFalse(result.stream().anyMatch(d -> d.getDriverId().equals(saved4.getDriverId())));

        // Cleanup
        driverRepository.delete(saved1);
        driverRepository.delete(saved2);
        driverRepository.delete(saved3);
        driverRepository.delete(saved4);
    }
}
