package com.ridelink.payment.controller;

import com.ridelink.payment.dto.ErrorResponse;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareFinalRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.service.FareCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fare Management", description = "Endpoints for estimating and calculating authoritative ride fares. Called by Ride Service with forwarded JWT.")
@SecurityRequirement(name = "bearerAuth")
public class FareController {

    private final FareCalculationService fareCalculationService;

    public FareController(FareCalculationService fareCalculationService) {
        this.fareCalculationService = fareCalculationService;
    }

    @Operation(summary = "Calculate estimated fare",
            description = "Calculates an estimated fare for a ride based on distance before the ride starts. " +
                    "Called by Ride Service during ride creation with forwarded caller JWT. " +
                    "Formula: fare = baseFare + (distanceKm × ratePerKm).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fare estimated successfully",
                    content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — missing rideId or invalid distanceKm",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/estimate")
    public ResponseEntity<FareResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        FareResponse response = fareCalculationService.estimateFare(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Calculate final authoritative fare",
            description = "Calculates the final authoritative fare when a ride is completed. " +
                    "Called by Ride Service during ride completion with forwarded caller JWT. " +
                    "Formula: fare = baseFare + (distanceKm × ratePerKm).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Final fare calculated successfully",
                    content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — missing rideId or invalid distanceKm",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/final")
    public ResponseEntity<FareResponse> calculateFinalFare(@Valid @RequestBody FareFinalRequest request) {
        FareResponse response = fareCalculationService.calculateFinalFare(request);
        return ResponseEntity.ok(response);
    }
}
