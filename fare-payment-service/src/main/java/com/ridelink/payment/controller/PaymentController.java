package com.ridelink.payment.controller;

import com.ridelink.payment.dto.ErrorResponse;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment & Receipt Management", description = "Endpoints for simulated payments and receipt generation")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create simulated payment", description = "Creates and processes a simulated payment (CASH or CARD_SIMULATED) for a completed ride. Available to authenticated PASSENGER, DRIVER, and ADMIN roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Simulated payment created with PAID or FAILED status",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid payment parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Associated fare not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Payment already completed for this ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get payment by ID", description = "Retrieves payment details by payment ID. Available to authenticated PASSENGER, DRIVER, and ADMIN roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "MongoDB payment ID", required = true) @PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get payment by ride ID", description = "Retrieves payment details for a specific ride ID. Available to authenticated PASSENGER, DRIVER, and ADMIN roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found for ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<PaymentResponse> getPaymentByRideId(
            @Parameter(description = "MongoDB ride ID", required = true) @PathVariable String rideId) {
        PaymentResponse response = paymentService.getPaymentByRideId(rideId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get receipt for payment", description = "Retrieves a combined receipt for a completed (PAID) payment. Available to authenticated PASSENGER, DRIVER, and ADMIN roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "409", description = "Payment is not in completed (PAID) state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment or associated fare not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @Parameter(description = "MongoDB payment ID", required = true) @PathVariable String paymentId) {
        ReceiptResponse response = paymentService.getReceiptByPaymentId(paymentId);
        return ResponseEntity.ok(response);
    }
}
