package com.ridelink.driver.repository;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
public class DriverRepositoryTest {

    private static final MongoServer MONGO_SERVER = new MongoServer(new MemoryBackend());
    private static final InetSocketAddress MONGO_ADDRESS = MONGO_SERVER.bind();

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://"
                + MONGO_ADDRESS.getHostString() + ":" + MONGO_ADDRESS.getPort() + "/driver_repository_test");
    }

    @AfterAll
    static void stopMongoServer() {
        MONGO_SERVER.shutdownNow();
    }

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

        List<Driver> availableDrivers = driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo");
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
    public void testFindByAccountIdAndExistsByAccountId() {
        Driver d1 = new Driver();
        d1.setAccountId("acc-lookup-1");
        d1.setLicenseNumber("DL-LK-1");
        d1.setServiceArea("Colombo");
        d1.setAvailability(DriverAvailability.AVAILABLE);

        Driver saved = driverRepository.save(d1);

        assertTrue(driverRepository.existsByAccountId("acc-lookup-1"));
        assertFalse(driverRepository.existsByAccountId("acc-nonexistent"));

        Optional<Driver> found = driverRepository.findByAccountId("acc-lookup-1");
        assertTrue(found.isPresent());
        assertEquals("acc-lookup-1", found.get().getAccountId());
        assertEquals("DL-LK-1", found.get().getLicenseNumber());

        // Cleanup
        driverRepository.delete(saved);
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
