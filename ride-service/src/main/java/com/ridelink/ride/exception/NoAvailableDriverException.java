package com.ridelink.ride.exception;

/**
 * Thrown when Driver Service returns an empty list of available drivers
 * during ride assignment.
 * Maps to 404 NOT_FOUND per API_CONTRACTS.md §5.5.
 */
public class NoAvailableDriverException extends RuntimeException {

    public NoAvailableDriverException(String serviceArea) {
        super("No available drivers found in area: " + serviceArea);
    }
}
