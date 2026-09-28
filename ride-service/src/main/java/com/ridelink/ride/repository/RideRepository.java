package com.ridelink.ride.repository;

import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for Ride documents.
 *
 * Supports:
 * - Standard CRUD via MongoRepository (findById, save, etc.)
 * - Passenger ride history lookup (§5.4 GET /api/rides/passenger/{passengerId})
 * - Status-based queries for future lifecycle operations
 */
@Repository
public interface RideRepository extends MongoRepository<Ride, String> {

    /**
     * Find all rides for a given passenger, newest first.
     * Used by GET /api/rides/passenger/{passengerId} (API_CONTRACTS.md §5.4).
     */
    List<Ride> findByPassengerIdOrderByCreatedAtDesc(String passengerId);

    /**
     * Find all rides with a given status.
     * Useful for future operations like finding all REQUESTED rides.
     */
    List<Ride> findByStatus(RideStatus status);
}
