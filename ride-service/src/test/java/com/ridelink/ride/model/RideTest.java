package com.ridelink.ride.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Ride domain model and RideStatus enum.
 * Verifies field mapping correctness and enum completeness.
 */
class RideTest {

    @Test
    @DisplayName("RideStatus enum contains all six contracted statuses")
    void rideStatusContainsAllContractedValues() {
        RideStatus[] values = RideStatus.values();
        assertEquals(6, values.length);
        assertNotNull(RideStatus.valueOf("REQUESTED"));
        assertNotNull(RideStatus.valueOf("ASSIGNED"));
        assertNotNull(RideStatus.valueOf("ACCEPTED"));
        assertNotNull(RideStatus.valueOf("IN_PROGRESS"));
        assertNotNull(RideStatus.valueOf("COMPLETED"));
        assertNotNull(RideStatus.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("Ride getters and setters work correctly for all fields")
    void rideGettersAndSettersWork() {
        Ride ride = new Ride();
        Instant now = Instant.now();

        ride.setId("rideId123");
        ride.setPassengerId("passengerId456");
        ride.setDriverId("driverId789");
        ride.setPickupLocation("Colombo Fort");
        ride.setDropoffLocation("Bambalapitiya");
        ride.setPickupLatitude(6.9340);
        ride.setPickupLongitude(79.8428);
        ride.setDropoffLatitude(6.8942);
        ride.setDropoffLongitude(79.8558);
        ride.setDistanceKm(5.5);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(475.00);
        ride.setFinalFare(null);
        ride.setFinalFareId("fare-final-123");
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);

        assertEquals("rideId123", ride.getId());
        assertEquals("passengerId456", ride.getPassengerId());
        assertEquals("driverId789", ride.getDriverId());
        assertEquals("Colombo Fort", ride.getPickupLocation());
        assertEquals("Bambalapitiya", ride.getDropoffLocation());
        assertEquals(6.9340, ride.getPickupLatitude());
        assertEquals(79.8428, ride.getPickupLongitude());
        assertEquals(6.8942, ride.getDropoffLatitude());
        assertEquals(79.8558, ride.getDropoffLongitude());
        assertEquals(5.5, ride.getDistanceKm());
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertEquals(475.00, ride.getEstimatedFare());
        assertNull(ride.getFinalFare());
        assertEquals("fare-final-123", ride.getFinalFareId());
        assertEquals(now, ride.getCreatedAt());
        assertEquals(now, ride.getUpdatedAt());
    }

    @Test
    @DisplayName("New Ride has null fields by default")
    void newRideHasNullDefaults() {
        Ride ride = new Ride();

        assertNull(ride.getId());
        assertNull(ride.getPassengerId());
        assertNull(ride.getDriverId());
        assertNull(ride.getStatus());
        assertNull(ride.getEstimatedFare());
        assertNull(ride.getFinalFare());
        assertNull(ride.getFinalFareId());
        assertNull(ride.getCreatedAt());
    }
}
