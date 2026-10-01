package com.ridelink.driver.security;

import com.ridelink.driver.domain.Driver;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.domain.Vehicle;
import com.ridelink.driver.domain.VehicleType;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests specifically verifying the resource ownership validation in DriverService and VehicleService.
 * Validates that authenticated drivers can only modify resources they own (Driver.accountId == JWT sub),
 * and attempting to modify another driver's resource throws AccessDeniedException (403).
 */
@ExtendWith(MockitoExtension.class)
class DriverOwnershipSecurityTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    private VehicleService vehicleService;

    private Driver driverOwnedByUser123;
    private Driver driverOwnedByUser456;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, driverRepository);

        driverOwnedByUser123 = new Driver();
        driverOwnedByUser123.setDriverId("driver-123");
        driverOwnedByUser123.setAccountId("user-123");
        driverOwnedByUser123.setLicenseNumber("DL-123");
        driverOwnedByUser123.setServiceArea("Colombo");
        driverOwnedByUser123.setAvailability(DriverAvailability.AVAILABLE);
        driverOwnedByUser123.setLatitude(6.9271);
        driverOwnedByUser123.setLongitude(79.8612);

        driverOwnedByUser456 = new Driver();
        driverOwnedByUser456.setDriverId("driver-456");
        driverOwnedByUser456.setAccountId("user-456");
        driverOwnedByUser456.setLicenseNumber("DL-456");
        driverOwnedByUser456.setServiceArea("Colombo");
        driverOwnedByUser456.setAvailability(DriverAvailability.AVAILABLE);
        driverOwnedByUser456.setLatitude(6.9271);
        driverOwnedByUser456.setLongitude(79.8612);

        // Set authenticated user to user-123 with ROLE_DRIVER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-123",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== DriverService.createDriver ownership tests ==========

    @Test
    void testCreateDriver_OwnAccountId_Succeeds() {
        CreateDriverRequest request = new CreateDriverRequest("user-123", "DL-123", "Colombo");
        when(driverRepository.existsByAccountId("user-123")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setDriverId("driver-123");
            return d;
        });

        DriverResponse response = driverService.createDriver(request);
        assertNotNull(response);
        assertEquals("user-123", response.getAccountId());
    }

    @Test
    void testCreateDriver_DifferentAccountId_ThrowsAccessDeniedException() {
        CreateDriverRequest request = new CreateDriverRequest("user-456", "DL-456", "Colombo");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> driverService.createDriver(request));
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    // ========== DriverService.updateAvailability ownership tests ==========

    @Test
    void testUpdateAvailability_OwnDriver_Succeeds() {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.UNAVAILABLE);
        when(driverRepository.findById("driver-123")).thenReturn(Optional.of(driverOwnedByUser123));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability("driver-123", request);
        assertNotNull(response);
        assertEquals(DriverAvailability.UNAVAILABLE, response.getAvailability());
    }

    @Test
    void testUpdateAvailability_OtherDriver_ThrowsAccessDeniedException() {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.UNAVAILABLE);
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driverOwnedByUser456));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> driverService.updateAvailability("driver-456", request));
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    // ========== DriverService.updateLocation ownership tests ==========

    @Test
    void testUpdateLocation_OwnDriver_Succeeds() {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9300, 79.8600);
        when(driverRepository.findById("driver-123")).thenReturn(Optional.of(driverOwnedByUser123));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateLocation("driver-123", request);
        assertNotNull(response);
        assertEquals(6.9300, response.getLatitude());
        assertEquals(79.8600, response.getLongitude());
    }

    @Test
    void testUpdateLocation_OtherDriver_ThrowsAccessDeniedException() {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9300, 79.8600);
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driverOwnedByUser456));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> driverService.updateLocation("driver-456", request));
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    // ========== VehicleService.createVehicle ownership tests ==========

    @Test
    void testCreateVehicle_OwnDriver_Succeeds() {
        VehicleRequest request = new VehicleRequest("WP-CAR-123", "Toyota", "Prius", 2022, "White", VehicleType.CAR);
        when(driverRepository.existsById("driver-123")).thenReturn(true);
        when(driverRepository.findById("driver-123")).thenReturn(Optional.of(driverOwnedByUser123));
        when(vehicleRepository.existsByDriverId("driver-123")).thenReturn(false);
        when(vehicleRepository.existsByLicensePlate("WP-CAR-123")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> {
            Vehicle v = inv.getArgument(0);
            v.setVehicleId("veh-123");
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle("driver-123", request);
        assertNotNull(response);
        assertEquals("driver-123", response.getDriverId());
    }

    @Test
    void testCreateVehicle_OtherDriver_ThrowsAccessDeniedException() {
        VehicleRequest request = new VehicleRequest("WP-CAR-123", "Toyota", "Prius", 2022, "White", VehicleType.CAR);
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driverOwnedByUser456));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> vehicleService.createVehicle("driver-456", request));
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // ========== VehicleService.updateVehicle ownership tests ==========

    @Test
    void testUpdateVehicle_OwnDriver_Succeeds() {
        VehicleRequest request = new VehicleRequest("WP-CAR-999", "Toyota", "Aqua", 2023, "Silver", VehicleType.CAR);
        Vehicle existingVehicle = new Vehicle();
        existingVehicle.setVehicleId("veh-123");
        existingVehicle.setDriverId("driver-123");
        existingVehicle.setLicensePlate("WP-CAR-123");

        when(driverRepository.existsById("driver-123")).thenReturn(true);
        when(driverRepository.findById("driver-123")).thenReturn(Optional.of(driverOwnedByUser123));
        when(vehicleRepository.findByDriverId("driver-123")).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.existsByLicensePlate("WP-CAR-999")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle("driver-123", request);
        assertNotNull(response);
        assertEquals("WP-CAR-999", response.getLicensePlate());
    }

    @Test
    void testUpdateVehicle_OtherDriver_ThrowsAccessDeniedException() {
        VehicleRequest request = new VehicleRequest("WP-CAR-999", "Toyota", "Aqua", 2023, "Silver", VehicleType.CAR);
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(driverRepository.findById("driver-456")).thenReturn(Optional.of(driverOwnedByUser456));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> vehicleService.updateVehicle("driver-456", request));
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
