package com.ridelink.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.GlobalExceptionHandler;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/payments with omitted simulateFailure returns PAID")
    void testCreatePaymentSuccess() throws Exception {
        Instant now = Instant.parse("2026-09-27T17:30:00.000Z");
        PaymentResponse response = new PaymentResponse("cc7f6a7b8c9d0e1f2a3b4c5d", "995c3d4e5f6a7b8c9d0e1f2a", "bb6e5f6a7b8c9d0e1f2a3b4c", 860.00, PaymentMethod.CASH, PaymentStatus.PAID, now);

        when(paymentService.createPayment(argThat(request -> !request.isSimulateFailure())))
                .thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
                                  "fareId": "bb6e5f6a7b8c9d0e1f2a3b4c",
                                  "amount": 860.00,
                                  "paymentMethod": "CASH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("cc7f6a7b8c9d0e1f2a3b4c5d"))
                .andExpect(jsonPath("$.rideId").value("995c3d4e5f6a7b8c9d0e1f2a"))
                .andExpect(jsonPath("$.fareId").value("bb6e5f6a7b8c9d0e1f2a3b4c"))
                .andExpect(jsonPath("$.amount").value(860.00))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/payments with simulateFailure=true returns persisted FAILED status")
    void testCreatePaymentSimulatedFailure() throws Exception {
        PaymentRequest request = new PaymentRequest(
                "ride-1", "fare-1", 860.00, PaymentMethod.CARD_SIMULATED, true);
        PaymentResponse response = new PaymentResponse(
                "failed-payment-1", "ride-1", "fare-1", 860.00,
                PaymentMethod.CARD_SIMULATED, PaymentStatus.FAILED, null);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("failed-payment-1"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.paidAt").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/payments with invalid amount should return 400 VALIDATION_ERROR")
    void testCreatePaymentInvalidAmount() throws Exception {
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", -50.0, PaymentMethod.CASH);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/payments duplicate should return 409 CONFLICT")
    void testCreatePaymentDuplicateConflict() throws Exception {
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 860.0, PaymentMethod.CASH);

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new ConflictException("Payment already completed for ride: ride-1"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} should return 200 OK")
    void testGetPaymentByIdSuccess() throws Exception {
        Instant now = Instant.parse("2026-09-27T17:30:00.000Z");
        PaymentResponse response = new PaymentResponse("payment-123", "ride-123", "fare-123", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, now);

        when(paymentService.getPaymentById("payment-123")).thenReturn(response);

        mockMvc.perform(get("/api/payments/payment-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment-123"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} returns persisted FAILED status")
    void testGetFailedPaymentByIdSuccess() throws Exception {
        PaymentResponse response = new PaymentResponse(
                "failed-payment-1", "ride-1", "fare-1", 860.0,
                PaymentMethod.CARD_SIMULATED, PaymentStatus.FAILED, null);

        when(paymentService.getPaymentById("failed-payment-1")).thenReturn(response);

        mockMvc.perform(get("/api/payments/failed-payment-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.paidAt").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} not found should return 404 NOT_FOUND")
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.getPaymentById("missing-id"))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: missing-id"));

        mockMvc.perform(get("/api/payments/missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/payments/ride/{rideId} should return 200 OK")
    void testGetPaymentByRideIdSuccess() throws Exception {
        Instant now = Instant.parse("2026-09-27T17:30:00.000Z");
        PaymentResponse response = new PaymentResponse("payment-123", "ride-123", "fare-123", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, now);

        when(paymentService.getPaymentByRideId("ride-123")).thenReturn(response);

        mockMvc.perform(get("/api/payments/ride/ride-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-123"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId}/receipt should return 200 OK with ReceiptResponse")
    void testGetReceiptSuccess() throws Exception {
        Instant now = Instant.parse("2026-09-27T17:30:00.000Z");
        ReceiptResponse receipt = new ReceiptResponse("cc7f6a7b8c9d0e1f2a3b4c5d", "995c3d4e5f6a7b8c9d0e1f2a", 13.2, 200.00, 50.00, 860.00, PaymentMethod.CASH, PaymentStatus.PAID, now);

        when(paymentService.getReceiptByPaymentId("cc7f6a7b8c9d0e1f2a3b4c5d")).thenReturn(receipt);

        mockMvc.perform(get("/api/payments/cc7f6a7b8c9d0e1f2a3b4c5d/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("cc7f6a7b8c9d0e1f2a3b4c5d"))
                .andExpect(jsonPath("$.rideId").value("995c3d4e5f6a7b8c9d0e1f2a"))
                .andExpect(jsonPath("$.distanceKm").value(13.2))
                .andExpect(jsonPath("$.baseFare").value(200.00))
                .andExpect(jsonPath("$.ratePerKm").value(50.00))
                .andExpect(jsonPath("$.totalFare").value(860.00))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/payments with non-existent fareId should return 404 NOT_FOUND")
    void testCreatePaymentFareNotFound() throws Exception {
        PaymentRequest request = new PaymentRequest("ride-1", "fare-non-existent", 860.0, PaymentMethod.CASH);

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Fare not found with id: fare-non-existent"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Fare not found with id: fare-non-existent"));
    }

    @Test
    @DisplayName("GET /api/payments/ride/{rideId} not found should return 404 NOT_FOUND")
    void testGetPaymentByRideIdNotFound() throws Exception {
        when(paymentService.getPaymentByRideId("missing-ride-id"))
                .thenThrow(new ResourceNotFoundException("Payment not found for ride id: missing-ride-id"));

        mockMvc.perform(get("/api/payments/ride/missing-ride-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId}/receipt for failed payment returns 409 CONFLICT")
    void testGetReceiptForFailedPayment() throws Exception {
        when(paymentService.getReceiptByPaymentId("failed-payment-id"))
                .thenThrow(new ConflictException("Receipt is only available for completed (PAID) payments"));

        mockMvc.perform(get("/api/payments/failed-payment-id/receipt"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
