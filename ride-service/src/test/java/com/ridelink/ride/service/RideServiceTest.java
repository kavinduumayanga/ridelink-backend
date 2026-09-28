package com.ridelink.ride.service;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for RideService.
 * Uses Mockito to isolate business logic from MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    private CreateRideRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateRideRequest();
        validRequest.setPassengerId("passenger123");
        validRequest.setPickupLocation("Colombo Fort");
        validRequest.setDropoffLocation("Bambalapitiya");
        validRequest.setPickupLatitude(6.9340);
        validRequest.setPickupLongitude(79.8428);
        validRequest.setDropoffLatitude(6.8942);
        validRequest.setDropoffLongitude(79.8558);
        validRequest.setDistanceKm(5.5);
    }

    // --- Ride Creation Tests ---

    @Test
    @DisplayName("createRide returns RideResponse with all request fields mapped")
    void createRide_success() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("generatedRideId");
            return ride;
        });

        RideResponse response = rideService.createRide(validRequest);

        assertNotNull(response);
        assertEquals("generatedRideId", response.getRideId());
        assertEquals("passenger123", response.getPassengerId());
        assertEquals("Colombo Fort", response.getPickupLocation());
        assertEquals("Bambalapitiya", response.getDropoffLocation());
        assertEquals(6.9340, response.getPickupLatitude());
        assertEquals(79.8428, response.getPickupLongitude());
        assertEquals(6.8942, response.getDropoffLatitude());
        assertEquals(79.8558, response.getDropoffLongitude());
        assertEquals(5.5, response.getDistanceKm());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("createRide sets status to REQUESTED")
    void createRide_statusIsRequested() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("rideId1");
            return ride;
        });

        RideResponse response = rideService.createRide(validRequest);

        assertEquals("REQUESTED", response.getStatus());
    }

    @Test
    @DisplayName("createRide sets driverId to null")
    void createRide_driverIdIsNull() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("rideId1");
            return ride;
        });

        RideResponse response = rideService.createRide(validRequest);

        assertNull(response.getDriverId());
    }

    @Test
    @DisplayName("createRide sets estimatedFare to null (Fare Service integration deferred)")
    void createRide_estimatedFareIsNull() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("rideId1");
            return ride;
        });

        RideResponse response = rideService.createRide(validRequest);

        assertNull(response.getEstimatedFare());
    }

    @Test
    @DisplayName("createRide sets finalFare to null")
    void createRide_finalFareIsNull() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("rideId1");
            return ride;
        });

        RideResponse response = rideService.createRide(validRequest);

        assertNull(response.getFinalFare());
    }

    @Test
    @DisplayName("createRide persists ride via repository.save()")
    void createRide_callsRepositorySave() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("rideId1");
            return ride;
        });

        rideService.createRide(validRequest);

        verify(rideRepository).save(argThat(ride ->
                ride.getPassengerId().equals("passenger123") &&
                ride.getStatus() == RideStatus.REQUESTED &&
                ride.getDriverId() == null
        ));
    }

    // --- Get Ride by ID Tests ---

    @Test
    @DisplayName("getRideById returns ride when it exists")
    void getRideById_found() {
        Ride ride = buildSampleRide("ride123", "passenger123");
        when(rideRepository.findById("ride123")).thenReturn(Optional.of(ride));

        RideResponse response = rideService.getRideById("ride123");

        assertNotNull(response);
        assertEquals("ride123", response.getRideId());
        assertEquals("passenger123", response.getPassengerId());
        assertEquals("REQUESTED", response.getStatus());
    }

    @Test
    @DisplayName("getRideById throws RideNotFoundException when ride does not exist")
    void getRideById_notFound() {
        when(rideRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () ->
                rideService.getRideById("nonexistent")
        );
    }

    // --- Get Rides by Passenger ID Tests ---

    @Test
    @DisplayName("getRidesByPassengerId returns rides for existing passenger")
    void getRidesByPassengerId_found() {
        Ride ride1 = buildSampleRide("ride1", "passenger123");
        Ride ride2 = buildSampleRide("ride2", "passenger123");
        when(rideRepository.findByPassengerIdOrderByCreatedAtDesc("passenger123"))
                .thenReturn(List.of(ride1, ride2));

        List<RideResponse> rides = rideService.getRidesByPassengerId("passenger123");

        assertEquals(2, rides.size());
        assertEquals("ride1", rides.get(0).getRideId());
        assertEquals("ride2", rides.get(1).getRideId());
    }

    @Test
    @DisplayName("getRidesByPassengerId returns empty list when no rides exist")
    void getRidesByPassengerId_empty() {
        when(rideRepository.findByPassengerIdOrderByCreatedAtDesc("unknown"))
                .thenReturn(Collections.emptyList());

        List<RideResponse> rides = rideService.getRidesByPassengerId("unknown");

        assertNotNull(rides);
        assertTrue(rides.isEmpty());
    }

    // --- Helper ---

    private Ride buildSampleRide(String id, String passengerId) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerId(passengerId);
        ride.setDriverId(null);
        ride.setPickupLocation("Colombo Fort");
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
}
