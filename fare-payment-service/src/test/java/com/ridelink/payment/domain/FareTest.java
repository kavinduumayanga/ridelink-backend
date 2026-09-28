package com.ridelink.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FareTest {

    @Test
    @DisplayName("Should create fare with all arguments and getters match")
    void testFareCreation() {
        Instant now = Instant.now();
        Fare fare = new Fare(
                "fare-123",
                "ride-456",
                FareType.ESTIMATE,
                12.5,
                200.00,
                50.00,
                825.00,
                now
        );

        assertEquals("fare-123", fare.getId());
        assertEquals("ride-456", fare.getRideId());
        assertEquals(FareType.ESTIMATE, fare.getFareType());
        assertEquals(12.5, fare.getDistanceKm());
        assertEquals(200.00, fare.getBaseFare());
        assertEquals(50.00, fare.getRatePerKm());
        assertEquals(825.00, fare.getTotalFare());
        assertEquals(now, fare.getCreatedAt());
    }

    @Test
    @DisplayName("Should support setters and no-arg constructor")
    void testFareSetters() {
        Fare fare = new Fare();
        fare.setId("fare-789");
        fare.setRideId("ride-999");
        fare.setFareType(FareType.FINAL);
        fare.setDistanceKm(13.2);
        fare.setBaseFare(200.00);
        fare.setRatePerKm(50.00);
        fare.setTotalFare(860.00);
        Instant now = Instant.now();
        fare.setCreatedAt(now);

        assertEquals("fare-789", fare.getId());
        assertEquals("ride-999", fare.getRideId());
        assertEquals(FareType.FINAL, fare.getFareType());
        assertEquals(13.2, fare.getDistanceKm());
        assertEquals(200.00, fare.getBaseFare());
        assertEquals(50.00, fare.getRatePerKm());
        assertEquals(860.00, fare.getTotalFare());
        assertEquals(now, fare.getCreatedAt());
    }

    @Test
    @DisplayName("Should obey equals and hashCode based on ID")
    void testFareEqualsAndHashCode() {
        Fare fare1 = new Fare("fare-123", "ride-1", FareType.ESTIMATE, 10.0, 200.0, 50.0, 700.0, Instant.now());
        Fare fare2 = new Fare("fare-123", "ride-2", FareType.FINAL, 15.0, 200.0, 50.0, 950.0, Instant.now());
        Fare fare3 = new Fare("fare-456", "ride-1", FareType.ESTIMATE, 10.0, 200.0, 50.0, 700.0, Instant.now());

        assertEquals(fare1, fare2);
        assertEquals(fare1.hashCode(), fare2.hashCode());
        assertNotEquals(fare1, fare3);
    }
}
