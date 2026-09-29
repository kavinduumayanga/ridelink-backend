package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideLifecycleServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private FareServiceClient fareServiceClient;

    @InjectMocks
    private RideService rideService;

    @Test
    void acceptRide_transitionsAssignedToAccepted() {
        Ride ride = ride("ride-1", RideStatus.ASSIGNED);
        stubSave(ride);

        RideResponse response = rideService.acceptRide("ride-1");

        assertEquals("ACCEPTED", response.getStatus());
        assertTrue(response.getUpdatedAt().isAfter(Instant.EPOCH));
    }

    @Test
    void acceptRide_fromWrongState_isRejected() {
        Ride ride = ride("ride-1", RideStatus.REQUESTED);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () -> rideService.acceptRide("ride-1"));
        verify(rideRepository, never()).save(any());
    }

    @Test
    void startRide_transitionsAcceptedToInProgress() {
        Ride ride = ride("ride-1", RideStatus.ACCEPTED);
        stubSave(ride);

        RideResponse response = rideService.startRide("ride-1");

        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void startRide_fromWrongState_isRejected() {
        Ride ride = ride("ride-1", RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () -> rideService.startRide("ride-1"));
        verify(rideRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"REQUESTED", "ASSIGNED", "ACCEPTED"})
    void cancelRide_fromCancellableState_transitionsToCancelled(RideStatus initialStatus) {
        Ride ride = ride("ride-1", initialStatus);
        stubSave(ride);

        RideResponse response = rideService.cancelRide("ride-1");

        assertEquals("CANCELLED", response.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"IN_PROGRESS", "COMPLETED", "CANCELLED"})
    void cancelRide_fromNonCancellableState_isRejected(RideStatus initialStatus) {
        Ride ride = ride("ride-1", initialStatus);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(InvalidRideStateException.class, () -> rideService.cancelRide("ride-1"));
        verify(rideRepository, never()).save(any());
    }

    @Test
    void lifecycleOperation_forMissingRide_returnsNotFound() {
        when(rideRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () -> rideService.acceptRide("missing"));
    }

    private void stubSave(Ride ride) {
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(rideRepository.save(ride)).thenReturn(ride);
    }

    private Ride ride(String id, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerId("passenger-1");
        ride.setDriverId("driver-1");
        ride.setStatus(status);
        ride.setCreatedAt(Instant.EPOCH);
        ride.setUpdatedAt(Instant.EPOCH);
        return ride;
    }
}
