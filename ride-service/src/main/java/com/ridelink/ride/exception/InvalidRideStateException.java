package com.ridelink.ride.exception;

/**
 * Thrown when a ride state transition is not valid.
 * Maps to 409 CONFLICT per API_CONTRACTS.md §1.3 and §5.1.
 */
public class InvalidRideStateException extends RuntimeException {

    public InvalidRideStateException(String message) {
        super(message);
    }
}
