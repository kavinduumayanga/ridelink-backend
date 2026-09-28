package com.ridelink.ride.service;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for ride creation and retrieval.
 *
 * Responsibilities:
 * - Create rides with status REQUESTED, driverId null, finalFare null.
 * - Retrieve a single ride by rideId (404 if not found).
 * - Retrieve passenger ride history, newest first.
 * - Map between Ride document and RideResponse DTO.
 *
 * estimatedFare is left null for now — Fare Service integration will
 * populate it in a later task (API_CONTRACTS.md §6.2).
 */
@Service
public class RideService {

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    /**
     * Creates a new ride in REQUESTED status.
     * driverId, estimatedFare, and finalFare are intentionally null.
     * estimatedFare will be populated by Fare Service integration later.
     */
    public RideResponse createRide(CreateRideRequest request) {
        Instant now = Instant.now();

        Ride ride = new Ride();
        ride.setPassengerId(request.getPassengerId());
        ride.setDriverId(null);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setDropoffLatitude(request.getDropoffLatitude());
        ride.setDropoffLongitude(request.getDropoffLongitude());
        ride.setDistanceKm(request.getDistanceKm());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(null);
        ride.setFinalFare(null);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);

        Ride saved = rideRepository.save(ride);
        return toRideResponse(saved);
    }

    /**
     * Retrieves a ride by its rideId.
     * @throws RideNotFoundException if ride does not exist (→ 404).
     */
    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        return toRideResponse(ride);
    }

    /**
     * Retrieves all rides for a passenger, newest first.
     * Returns an empty list if no rides exist (never 404).
     */
    public List<RideResponse> getRidesByPassengerId(String passengerId) {
        List<Ride> rides = rideRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        return rides.stream()
                .map(this::toRideResponse)
                .toList();
    }

    /**
     * Maps a Ride document to a RideResponse DTO.
     * Document `id` becomes `rideId` in the response.
     */
    private RideResponse toRideResponse(Ride ride) {
        RideResponse response = new RideResponse();
        response.setRideId(ride.getId());
        response.setPassengerId(ride.getPassengerId());
        response.setDriverId(ride.getDriverId());
        response.setPickupLocation(ride.getPickupLocation());
        response.setDropoffLocation(ride.getDropoffLocation());
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setDropoffLatitude(ride.getDropoffLatitude());
        response.setDropoffLongitude(ride.getDropoffLongitude());
        response.setDistanceKm(ride.getDistanceKm());
        response.setStatus(ride.getStatus().name());
        response.setEstimatedFare(ride.getEstimatedFare());
        response.setFinalFare(ride.getFinalFare());
        response.setCreatedAt(ride.getCreatedAt());
        response.setUpdatedAt(ride.getUpdatedAt());
        return response;
    }
}
