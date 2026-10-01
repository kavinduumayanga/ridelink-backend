package com.ridelink.ride.model;

/**
 * Ride lifecycle states as defined in API_CONTRACTS.md §1.4 and §5.1.
 *
 * Valid transitions:
 *   REQUESTED  → ASSIGNED | CANCELLED
 *   ASSIGNED   → ACCEPTED | CANCELLED
 *   ACCEPTED   → IN_PROGRESS | CANCELLED
 *   IN_PROGRESS → COMPLETED
 *
 * Terminal states: COMPLETED, CANCELLED
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
