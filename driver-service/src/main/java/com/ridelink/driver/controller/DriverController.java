package com.ridelink.driver.controller;

import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.ErrorResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver & Vehicle", description = "Manage driver operational profiles, vehicles, availability, and simulated location")
public class DriverController {

    private final DriverService driverService;
    private final VehicleService vehicleService;

    public DriverController(DriverService driverService, VehicleService vehicleService) {
        this.driverService = driverService;
        this.vehicleService = vehicleService;
    }

    @Operation(
            summary = "Create driver operational profile",
            description = "Creates the operational profile for a user who registered with role DRIVER in Account Service. "
                    + "The accountId in the request must match the JWT sub (userId). Default availability is UNAVAILABLE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver profile created",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role or accountId does not match JWT sub",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Driver profile already exists for this accountId",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get available drivers",
            description = "Returns all drivers with availability = AVAILABLE in the given serviceArea. "
                    + "Results are ordered by driverId ascending for deterministic selection. "
                    + "Returns an empty array [] if no drivers are available. Never returns 404 for an empty list. "
                    + "Consumed by Ride Service during driver assignment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of available drivers (may be empty)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Missing serviceArea query parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @Parameter(description = "City or zone name — exact match", required = true, example = "Colombo")
            @RequestParam String serviceArea) {
        List<DriverResponse> response = driverService.getAvailableDrivers(serviceArea);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get driver by ID",
            description = "Returns the driver operational profile. Any authenticated user can read a driver profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver found",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable String driverId) {
        DriverResponse response = driverService.getDriverById(driverId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create vehicle for driver",
            description = "Registers a vehicle for the specified driver. Requires DRIVER role and ownership of the driver profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle created",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner of this driver profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Vehicle already exists for this driver or duplicate license plate",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{driverId}/vehicle")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable String driverId,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(driverId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update vehicle for driver",
            description = "Updates the vehicle registered to the specified driver. Requires DRIVER role and ownership of the driver profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner of this driver profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver or vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{driverId}/vehicle")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable String driverId,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(driverId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update driver availability",
            description = "Sets the driver's availability to AVAILABLE or UNAVAILABLE. Requires DRIVER role and ownership of the driver profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability updated — returns full driver DTO",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid availability value)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner of this driver profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable String driverId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        DriverResponse response = driverService.updateAvailability(driverId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update simulated location",
            description = "Sets the driver's simulated GPS coordinates. Latitude must be -90.0 to 90.0, longitude must be -180.0 to 180.0. "
                    + "Requires DRIVER role and ownership of the driver profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated — returns full driver DTO",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (coordinates out of range)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner of this driver profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable String driverId,
            @Valid @RequestBody UpdateLocationRequest request) {
        DriverResponse response = driverService.updateLocation(driverId, request);
        return ResponseEntity.ok(response);
    }
}
