package com.ridelink.driver.service;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    private CreateDriverRequest createRequest;
    private Driver driver;

    @BeforeEach
    void setUp() {
        createRequest = new CreateDriverRequest("acc-123", "DL-998877", "Colombo");

        driver = new Driver();
        driver.setDriverId("driver-456");
        driver.setAccountId("acc-123");
        driver.setLicenseNumber("DL-998877");
        driver.setServiceArea("Colombo");
        driver.setAvailability(DriverAvailability.UNAVAILABLE);
        driver.setLatitude(0.0);
        driver.setLongitude(0.0);
    }

    @Test
    void testCreateDriver_Success() {
        when(driverRepository.existsByAccountId("acc-123")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver d = invocation.getArgument(0);
            d.setDriverId("driver-456");
            return d;
        });

        DriverResponse response = driverService.createDriver(createRequest);

        assertNotNull(response);
        assertEquals("driver-456", response.getDriverId());
        assertEquals("acc-123", response.getAccountId());
        assertEquals("DL-998877", response.getLicenseNumber());
        assertEquals("Colombo", response.getServiceArea());
        assertEquals(DriverAvailability.UNAVAILABLE, response.getAvailability());
        assertEquals(0.0, response.getLatitude());
        assertEquals(0.0, response.getLongitude());

        verify(driverRepository, times(1)).existsByAccountId("acc-123");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void testCreateDriver_DuplicateAccountId_ThrowsException() {
        when(driverRepository.existsByAccountId("acc-123")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> driverService.createDriver(createRequest)
        );

        assertTrue(exception.getMessage().contains("Driver profile already exists"));
        verify(driverRepository, times(1)).existsByAccountId("acc-123");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void testGetDriverById_Success() {
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.getDriverById("driver-456");

        assertNotNull(response);
        assertEquals("driver-456", response.getDriverId());
        assertEquals("acc-123", response.getAccountId());
        assertEquals("DL-998877", response.getLicenseNumber());
        assertEquals("Colombo", response.getServiceArea());
    }

    @Test
    void testGetDriverById_NotFound_ThrowsException() {
        when(driverRepository.findById("driver-999")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.getDriverById("driver-999")
        );

        assertTrue(exception.getMessage().contains("Driver not found"));
    }
}
