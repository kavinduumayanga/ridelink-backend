package com.ridelink.driver.service;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @Test
    void testUpdateAvailability_Available_Success() {
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);
        DriverResponse response = driverService.updateAvailability("driver-456", request);

        assertNotNull(response);
        assertEquals(DriverAvailability.AVAILABLE, response.getAvailability());
        assertEquals(DriverAvailability.AVAILABLE, driver.getAvailability());
        assertEquals("driver-456", response.getDriverId());
        assertEquals("acc-123", response.getAccountId());
        assertEquals("DL-998877", response.getLicenseNumber());
        assertEquals("Colombo", response.getServiceArea());
        assertEquals(0.0, response.getLatitude());
        assertEquals(0.0, response.getLongitude());
        verify(driverRepository, times(1)).save(driver);
    }

    @Test
    void testUpdateAvailability_Unavailable_Success() {
        driver.setAvailability(DriverAvailability.AVAILABLE);
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.UNAVAILABLE);
        DriverResponse response = driverService.updateAvailability("driver-456", request);

        assertNotNull(response);
        assertEquals(DriverAvailability.UNAVAILABLE, response.getAvailability());
        assertEquals(DriverAvailability.UNAVAILABLE, driver.getAvailability());
        verify(driverRepository, times(1)).save(driver);
    }

    @Test
    void testUpdateAvailability_NotFound_ThrowsException() {
        when(driverRepository.findById("driver-999")).thenReturn(Optional.empty());

        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.updateAvailability("driver-999", request)
        );

        assertTrue(exception.getMessage().contains("Driver not found"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void testUpdateLocation_Success() {
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);
        DriverResponse response = driverService.updateLocation("driver-456", request);

        assertNotNull(response);
        assertEquals(6.9271, response.getLatitude());
        assertEquals(79.8612, response.getLongitude());
        assertEquals(6.9271, driver.getLatitude());
        assertEquals(79.8612, driver.getLongitude());
        assertEquals("driver-456", response.getDriverId());
        assertEquals("acc-123", response.getAccountId());
        assertEquals("DL-998877", response.getLicenseNumber());
        assertEquals("Colombo", response.getServiceArea());
        assertEquals(DriverAvailability.UNAVAILABLE, response.getAvailability());
        verify(driverRepository, times(1)).save(driver);
    }

    @Test
    void testUpdateLocation_NotFound_ThrowsException() {
        when(driverRepository.findById("driver-999")).thenReturn(Optional.empty());

        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.updateLocation("driver-999", request)
        );

        assertTrue(exception.getMessage().contains("Driver not found"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void testGetAvailableDrivers_WithServiceArea_ReturnsMatchingAvailableDrivers() {
        Driver d1 = new Driver();
        d1.setDriverId("driver-01");
        d1.setAccountId("acc-1");
        d1.setLicenseNumber("DL-111");
        d1.setServiceArea("Colombo");
        d1.setAvailability(DriverAvailability.AVAILABLE);
        d1.setLatitude(6.9271);
        d1.setLongitude(79.8612);

        Driver d2 = new Driver();
        d2.setDriverId("driver-02");
        d2.setAccountId("acc-2");
        d2.setLicenseNumber("DL-222");
        d2.setServiceArea("Colombo");
        d2.setAvailability(DriverAvailability.AVAILABLE);
        d2.setLatitude(6.9300);
        d2.setLongitude(79.8650);

        when(driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo"))
                .thenReturn(List.of(d1, d2));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("driver-01", result.get(0).getDriverId());
        assertEquals(DriverAvailability.AVAILABLE, result.get(0).getAvailability());
        assertEquals("Colombo", result.get(0).getServiceArea());
        assertEquals("driver-02", result.get(1).getDriverId());
        assertEquals(DriverAvailability.AVAILABLE, result.get(1).getAvailability());
        assertEquals("Colombo", result.get(1).getServiceArea());

        verify(driverRepository, times(1))
                .findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo");
    }

    @Test
    void testGetAvailableDrivers_ExcludesUnavailableDrivers() {
        when(driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo"))
                .thenReturn(List.of());

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(driverRepository, times(1))
                .findByAvailabilityAndServiceAreaOrderByDriverIdAsc(eq(DriverAvailability.AVAILABLE), eq("Colombo"));
    }

    @Test
    void testGetAvailableDrivers_DeterministicOrdering() {
        Driver d1 = new Driver();
        d1.setDriverId("driver-01");
        d1.setAvailability(DriverAvailability.AVAILABLE);
        d1.setServiceArea("Colombo");

        Driver d2 = new Driver();
        d2.setDriverId("driver-02");
        d2.setAvailability(DriverAvailability.AVAILABLE);
        d2.setServiceArea("Colombo");

        Driver d3 = new Driver();
        d3.setDriverId("driver-03");
        d3.setAvailability(DriverAvailability.AVAILABLE);
        d3.setServiceArea("Colombo");

        when(driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo"))
                .thenReturn(List.of(d1, d2, d3));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo");

        assertEquals(3, result.size());
        assertEquals("driver-01", result.get(0).getDriverId());
        assertEquals("driver-02", result.get(1).getDriverId());
        assertEquals("driver-03", result.get(2).getDriverId());

        verify(driverRepository, times(1))
                .findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo");
    }

    @Test
    void testGetAvailableDrivers_NoEligibleDrivers_ReturnsEmptyList() {
        when(driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Negombo"))
                .thenReturn(List.of());

        List<DriverResponse> result = driverService.getAvailableDrivers("Negombo");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAvailableDrivers_ResponseContainsRequiredDriverId() {
        Driver d = new Driver();
        d.setDriverId("driver-req-id");
        d.setAccountId("acc-req");
        d.setLicenseNumber("DL-REQ");
        d.setServiceArea("Colombo");
        d.setAvailability(DriverAvailability.AVAILABLE);
        d.setLatitude(6.9271);
        d.setLongitude(79.8612);

        when(driverRepository.findByAvailabilityAndServiceAreaOrderByDriverIdAsc(DriverAvailability.AVAILABLE, "Colombo"))
                .thenReturn(List.of(d));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo");

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDriverId());
        assertEquals("driver-req-id", result.get(0).getDriverId());
    }
}
