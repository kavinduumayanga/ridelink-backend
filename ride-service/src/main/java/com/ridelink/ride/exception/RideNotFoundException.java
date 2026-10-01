package com.ridelink.ride.exception;

/**
 * Thrown when a requested ride does not exist.
 * Maps to 404 NOT_FOUND per API_CONTRACTS.md §1.3.
 */
public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(String rideId) {
        super("Ride not found: " + rideId);
    }
}
