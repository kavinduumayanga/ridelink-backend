package com.ridelink.driver.service;

import com.ridelink.driver.domain.Vehicle;
import com.ridelink.driver.domain.VehicleType;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.repository.VehicleRepository;
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
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private VehicleRequest vehicleRequest;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleRequest = new VehicleRequest("ABC-1234", "Toyota", "Prius", 2022, "White", VehicleType.CAR);

        vehicle = new Vehicle();
        vehicle.setVehicleId("veh-100");
        vehicle.setDriverId("driver-456");
        vehicle.setLicensePlate("ABC-1234");
        vehicle.setMake("Toyota");
        vehicle.setModel("Prius");
        vehicle.setYear(2022);
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
    }

    @Test
    void testCreateVehicle_Success() {
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.existsByDriverId("driver-456")).thenReturn(false);
        when(vehicleRepository.existsByLicensePlate("ABC-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setVehicleId("veh-100");
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle("driver-456", vehicleRequest);

        assertNotNull(response);
        assertEquals("veh-100", response.getVehicleId());
        assertEquals("driver-456", response.getDriverId());
        assertEquals("ABC-1234", response.getLicensePlate());
        assertEquals("Toyota", response.getMake());
        assertEquals("Prius", response.getModel());
        assertEquals(2022, response.getYear());
        assertEquals("White", response.getColor());
        assertEquals(VehicleType.CAR, response.getVehicleType());

        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void testCreateVehicle_DriverNotFound_ThrowsException() {
        when(driverRepository.existsById("driver-999")).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.createVehicle("driver-999", vehicleRequest)
        );

        assertTrue(exception.getMessage().contains("Driver not found"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testCreateVehicle_VehicleAlreadyExistsForDriver_ThrowsException() {
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.existsByDriverId("driver-456")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> vehicleService.createVehicle("driver-456", vehicleRequest)
        );

        assertTrue(exception.getMessage().contains("Vehicle already registered"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testCreateVehicle_DuplicateLicensePlate_ThrowsException() {
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.existsByDriverId("driver-456")).thenReturn(false);
        when(vehicleRepository.existsByLicensePlate("ABC-1234")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> vehicleService.createVehicle("driver-456", vehicleRequest)
        );

        assertTrue(exception.getMessage().contains("Vehicle with license plate already exists"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testUpdateVehicle_Success() {
        VehicleRequest updateReq = new VehicleRequest("ABC-9999", "Honda", "Civic", 2023, "Black", VehicleType.CAR);

        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.findByDriverId("driver-456")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByLicensePlate("ABC-9999")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle("driver-456", updateReq);

        assertNotNull(response);
        assertEquals("ABC-9999", response.getLicensePlate());
        assertEquals("Honda", response.getMake());
        assertEquals("Civic", response.getModel());
        assertEquals(2023, response.getYear());
        assertEquals("Black", response.getColor());

        verify(vehicleRepository, times(1)).save(vehicle);
    }

    @Test
    void testUpdateVehicle_DriverNotFound_ThrowsException() {
        when(driverRepository.existsById("driver-999")).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.updateVehicle("driver-999", vehicleRequest)
        );

        assertTrue(exception.getMessage().contains("Driver not found"));
    }

    @Test
    void testUpdateVehicle_VehicleNotFound_ThrowsException() {
        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.findByDriverId("driver-456")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.updateVehicle("driver-456", vehicleRequest)
        );

        assertTrue(exception.getMessage().contains("Vehicle not found"));
    }

    @Test
    void testUpdateVehicle_DuplicateLicensePlate_ThrowsException() {
        VehicleRequest updateReq = new VehicleRequest("XYZ-9999", "Honda", "Civic", 2023, "Black", VehicleType.CAR);

        when(driverRepository.existsById("driver-456")).thenReturn(true);
        when(vehicleRepository.findByDriverId("driver-456")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByLicensePlate("XYZ-9999")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> vehicleService.updateVehicle("driver-456", updateReq)
        );

        assertTrue(exception.getMessage().contains("Vehicle with license plate already exists"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
