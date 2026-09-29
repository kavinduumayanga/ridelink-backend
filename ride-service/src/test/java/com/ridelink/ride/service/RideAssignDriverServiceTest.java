package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.DriverServiceException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RideService.assignDriver().
 * Uses Mockito to mock Driver Service and MongoDB.
 * Driver Service does NOT need to be running.
 */
@ExtendWith(MockitoExtension.class)
class RideAssignDriverServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private FareServiceClient fareServiceClient;

    @InjectMocks
    private RideService rideService;

    // --- Happy Path ---

    @Test
    @DisplayName("REQUESTED ride + one available driver → assigned with that driverId")
    void assignDriver_requestedRideOneDriver_assigned() {
        Ride ride = buildRequestedRide("ride1", "Colombo Fort");
        when(rideRepository.findById("ride1")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(List.of(buildDriver("driver1")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.assignDriver("ride1");

        assertEquals("ASSIGNED", response.getStatus());
        assertEquals("driver1", response.getDriverId());
        verify(rideRepository).save(any(Ride.class));
    }

    @Test
    @DisplayName("REQUESTED ride + multiple drivers → FIRST driver selected")
    void assignDriver_multipleDrivers_firstSelected() {
        Ride ride = buildRequestedRide("ride2", "Colombo Fort");
        when(rideRepository.findById("ride2")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(List.of(
                        buildDriver("driverFirst"),
                        buildDriver("driverSecond"),
                        buildDriver("driverThird")
                ));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.assignDriver("ride2");

        assertEquals("driverFirst", response.getDriverId());
    }

    @Test
    @DisplayName("Selected driverId is persisted in the saved ride")
    void assignDriver_driverIdPersisted() {
        Ride ride = buildRequestedRide("ride3", "Colombo Fort");
        when(rideRepository.findById("ride3")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(List.of(buildDriver("driverPersisted")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        rideService.assignDriver("ride3");

        verify(rideRepository).save(argThat(savedRide ->
                "driverPersisted".equals(savedRide.getDriverId())
        ));
    }

    @Test
    @DisplayName("Status changes to ASSIGNED after assignment")
    void assignDriver_statusChangesToAssigned() {
        Ride ride = buildRequestedRide("ride4", "Colombo Fort");
        when(rideRepository.findById("ride4")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(List.of(buildDriver("driver1")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        rideService.assignDriver("ride4");

        verify(rideRepository).save(argThat(savedRide ->
                savedRide.getStatus() == RideStatus.ASSIGNED
        ));
    }

    @Test
    @DisplayName("updatedAt changes after assignment")
    void assignDriver_updatedAtChanges() {
        Instant originalUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        Ride ride = buildRequestedRide("ride5", "Colombo Fort");
        ride.setUpdatedAt(originalUpdatedAt);
        when(rideRepository.findById("ride5")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(List.of(buildDriver("driver1")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.assignDriver("ride5");

        assertNotNull(response.getUpdatedAt());
        assertNotEquals(originalUpdatedAt, response.getUpdatedAt());
    }

    // --- No Available Drivers ---

    @Test
    @DisplayName("No available drivers → NoAvailableDriverException thrown")
    void assignDriver_noDrivers_throwsException() {
        Ride ride = buildRequestedRide("ride6", "Colombo Fort");
        when(rideRepository.findById("ride6")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(Collections.emptyList());

        assertThrows(NoAvailableDriverException.class, () ->
                rideService.assignDriver("ride6")
        );
    }

    @Test
    @DisplayName("Ride remains unchanged when no driver exists")
    void assignDriver_noDrivers_rideUnchanged() {
        Ride ride = buildRequestedRide("ride7", "Colombo Fort");
        Instant originalUpdatedAt = ride.getUpdatedAt();
        when(rideRepository.findById("ride7")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenReturn(Collections.emptyList());

        assertThrows(NoAvailableDriverException.class, () ->
                rideService.assignDriver("ride7")
        );

        // Ride should not be saved
        verify(rideRepository, never()).save(any(Ride.class));
        // Ride state should remain unchanged
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertNull(ride.getDriverId());
        assertEquals(originalUpdatedAt, ride.getUpdatedAt());
    }

    // --- Invalid State Transitions ---

    @Test
    @DisplayName("Assignment attempted from ASSIGNED → 409")
    void assignDriver_fromAssigned_throws409() {
        Ride ride = buildRideWithStatus("ride8", RideStatus.ASSIGNED);
        when(rideRepository.findById("ride8")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () ->
                rideService.assignDriver("ride8")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Assignment attempted from ACCEPTED → 409")
    void assignDriver_fromAccepted_throws409() {
        Ride ride = buildRideWithStatus("ride9", RideStatus.ACCEPTED);
        when(rideRepository.findById("ride9")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () ->
                rideService.assignDriver("ride9")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Assignment attempted from IN_PROGRESS → 409")
    void assignDriver_fromInProgress_throws409() {
        Ride ride = buildRideWithStatus("ride10", RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride10")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () ->
                rideService.assignDriver("ride10")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Assignment attempted from COMPLETED → 409")
    void assignDriver_fromCompleted_throws409() {
        Ride ride = buildRideWithStatus("ride11", RideStatus.COMPLETED);
        when(rideRepository.findById("ride11")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () ->
                rideService.assignDriver("ride11")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Assignment attempted from CANCELLED → 409")
    void assignDriver_fromCancelled_throws409() {
        Ride ride = buildRideWithStatus("ride12", RideStatus.CANCELLED);
        when(rideRepository.findById("ride12")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () ->
                rideService.assignDriver("ride12")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // --- Ride Not Found ---

    @Test
    @DisplayName("Ride not found → RideNotFoundException (404)")
    void assignDriver_rideNotFound_throws404() {
        when(rideRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () ->
                rideService.assignDriver("nonexistent")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // --- Driver Service Unavailable ---

    @Test
    @DisplayName("Driver Service unavailable → DriverServiceException (clean 5xx)")
    void assignDriver_driverServiceDown_throwsCleanException() {
        Ride ride = buildRequestedRide("ride13", "Colombo Fort");
        when(rideRepository.findById("ride13")).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers("Colombo Fort"))
                .thenThrow(new DriverServiceException("Connection refused"));

        assertThrows(DriverServiceException.class, () ->
                rideService.assignDriver("ride13")
        );
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // --- Helpers ---

    private Ride buildRequestedRide(String id, String pickupLocation) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerId("passenger123");
        ride.setDriverId(null);
        ride.setPickupLocation(pickupLocation);
        ride.setDropoffLocation("Bambalapitiya");
        ride.setPickupLatitude(6.9340);
        ride.setPickupLongitude(79.8428);
        ride.setDropoffLatitude(6.8942);
        ride.setDropoffLongitude(79.8558);
        ride.setDistanceKm(5.5);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(null);
        ride.setFinalFare(null);
        ride.setCreatedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return ride;
    }

    private Ride buildRideWithStatus(String id, RideStatus status) {
        Ride ride = buildRequestedRide(id, "Colombo Fort");
        ride.setStatus(status);
        if (status != RideStatus.REQUESTED) {
            ride.setDriverId("existingDriver");
        }
        return ride;
    }

    private AvailableDriverResponse buildDriver(String driverId) {
        AvailableDriverResponse driver = new AvailableDriverResponse();
        driver.setDriverId(driverId);
        driver.setAccountId("account-" + driverId);
        driver.setLicenseNumber("DL-123");
        driver.setServiceArea("Colombo Fort");
        driver.setAvailability("AVAILABLE");
        driver.setLatitude(6.9271);
        driver.setLongitude(79.8612);
        return driver;
    }
}
