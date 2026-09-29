package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for ride endpoints.
 * Paths match API_CONTRACTS.md §5.2–§5.8 exactly.
 *
 * POST  /api/rides                          → Create Ride (§5.2)
 * GET   /api/rides/{rideId}                 → Get Ride (§5.3)
 * GET   /api/rides/passenger/{passengerId}  → Get Passenger Rides (§5.4)
 * PATCH /api/rides/{rideId}/assign          → Assign Driver (§5.5)
 * PATCH /api/rides/{rideId}/complete        → Complete Ride (§5.8)
 */
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * POST /api/rides — Create a new ride request.
     * Calls Fare Service for estimated fare during creation.
     * Returns 201 Created with the ride DTO including estimatedFare.
     */
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER') and #request.passengerId == authentication.name")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/rides/{rideId} — Retrieve a single ride.
     * Returns 200 OK or 404 if not found.
     */
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRide(@PathVariable String rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rides/passenger/{passengerId} — Retrieve all rides for a passenger.
     * Returns 200 OK with a list (empty array if no rides).
     */
    @GetMapping("/passenger/{passengerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PASSENGER') and #passengerId == authentication.name)")
    public ResponseEntity<List<RideResponse>> getPassengerRides(@PathVariable String passengerId) {
        List<RideResponse> rides = rideService.getRidesByPassengerId(passengerId);
        return ResponseEntity.ok(rides);
    }

    /**
     * PATCH /api/rides/{rideId}/assign — Assign a driver to a ride.
     * Ride Service orchestrates: queries Driver Service for available drivers,
     * selects the first eligible driver, and assigns that driverId to the ride.
     * No driverId is supplied in the request body.
     * Returns 200 OK with the updated ride DTO (status: ASSIGNED, driverId populated).
     *
     * Error codes: 404 ride not found, 404 no available drivers, 409 invalid state, 502 Driver Service down.
     */
    @PatchMapping("/{rideId}/assign")
    public ResponseEntity<RideResponse> assignDriver(@PathVariable String rideId) {
        RideResponse response = rideService.assignDriver(rideId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/rides/{rideId}/complete — Complete a ride.
     * Ride Service calls Fare Service for final fare calculation,
     * transitions status IN_PROGRESS → COMPLETED, and stores finalFare.
     * Returns 200 OK with the updated ride DTO (status: COMPLETED, finalFare populated).
     *
     * Error codes: 404 ride not found, 409 invalid state, 502 Fare Service down.
     */
    @PatchMapping("/{rideId}/complete")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String rideId) {
        RideResponse response = rideService.completeRide(rideId);
        return ResponseEntity.ok(response);
    }
}
