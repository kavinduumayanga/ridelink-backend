package com.ridelink.ride.controller;

import com.ridelink.ride.config.OpenApiConfig;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.ErrorResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Paths match API_CONTRACTS.md §5.2–§5.9 exactly.
 *
 * POST  /api/rides                          → Create Ride (§5.2)
 * GET   /api/rides/{rideId}                 → Get Ride (§5.3)
 * GET   /api/rides/passenger/{passengerId}  → Get Passenger Rides (§5.4)
 * PATCH /api/rides/{rideId}/assign          → Assign Driver (§5.5)
 * PATCH /api/rides/{rideId}/accept          → Accept Ride (§5.6)
 * PATCH /api/rides/{rideId}/start           → Start Ride (§5.7)
 * PATCH /api/rides/{rideId}/complete        → Complete Ride (§5.8)
 * PATCH /api/rides/{rideId}/cancel          → Cancel Ride (§5.9)
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Rides", description = "Ride creation, retrieval, assignment, and lifecycle operations")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
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
    @Operation(summary = "Create ride", description = "Creates a REQUESTED ride for the authenticated PASSENGER and obtains an estimated fare.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ride created",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not a PASSENGER or passengerId does not match JWT subject",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Fare Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> createRide(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Ride request; passengerId must match the authenticated JWT subject")
            @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/rides/{rideId} — Retrieve a single ride.
     * Returns 200 OK or 404 if not found.
     */
    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride", description = "Returns one ride. Any authenticated RideLink role may call this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride found",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> getRide(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rides/passenger/{passengerId} — Retrieve all rides for a passenger.
     * Returns 200 OK with a list (empty array if no rides).
     */
    @GetMapping("/passenger/{passengerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PASSENGER') and #passengerId == authentication.name)")
    @Operation(summary = "Get passenger rides", description = "Returns the authenticated PASSENGER's rides newest first, or any passenger's rides for ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride history returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Passenger does not own the requested history",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RideResponse>> getPassengerRides(
            @Parameter(description = "Account Service user ID of the passenger", required = true)
            @PathVariable String passengerId) {
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
    @Operation(summary = "Assign driver", description = "Assigns the first eligible Driver Service driver. Allowed for the ride's PASSENGER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver assigned",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller cannot manage this ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride or available driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride is not REQUESTED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Driver Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> assignDriver(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        RideResponse response = rideService.assignDriver(rideId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/accept")
    @Operation(summary = "Accept ride", description = "Transitions ASSIGNED to ACCEPTED. Requires the assigned DRIVER account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride accepted",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the assigned DRIVER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride is not ASSIGNED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> acceptRide(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.acceptRide(rideId));
    }

    @PatchMapping("/{rideId}/start")
    @Operation(summary = "Start ride", description = "Transitions ACCEPTED to IN_PROGRESS. Requires the assigned DRIVER account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride started",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the assigned DRIVER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride is not ACCEPTED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> startRide(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.startRide(rideId));
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
    @Operation(summary = "Complete ride", description = "Transitions IN_PROGRESS to COMPLETED and obtains the authoritative final fare. Requires the assigned DRIVER account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride completed",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the assigned DRIVER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride is not IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Fare Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> completeRide(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        RideResponse response = rideService.completeRide(rideId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel ride", description = "Transitions REQUESTED, ASSIGNED, or ACCEPTED to CANCELLED. Allowed for the ride's PASSENGER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride cancelled",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller cannot manage this ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ride is terminal or cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RideResponse> cancelRide(
            @Parameter(description = "Ride Service ride ID", required = true)
            @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.cancelRide(rideId));
    }
}
