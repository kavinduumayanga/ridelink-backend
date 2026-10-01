package com.ridelink.payment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridelink.payment.config.FareProperties;
import com.ridelink.payment.config.SecurityConfig;
import com.ridelink.payment.controller.FareController;
import com.ridelink.payment.controller.PaymentController;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareFinalRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.service.FareCalculationService;
import com.ridelink.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PaymentController.class, FareController.class})
@Import({SecurityConfig.class, JwtUtils.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private FareCalculationService fareCalculationService;

    @MockitoBean
    private FareProperties fareProperties;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Protected payment endpoint without JWT should return 401 UNAUTHENTICATED")
    void testPaymentWithoutJwtUnauthorized() throws Exception {
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 500.0, PaymentMethod.CASH);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Missing or invalid JWT token"))
                .andExpect(jsonPath("$.path").value("/api/payments"));
    }

    @Test
    @DisplayName("Protected payment endpoint with expired JWT should return 401 UNAUTHENTICATED")
    void testPaymentWithExpiredJwtUnauthorized() throws Exception {
        String expiredToken = JwtTestHelper.generateExpiredToken("user-1", "PASSENGER");
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 500.0, PaymentMethod.CASH);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Protected payment endpoint with malformed JWT should return 401 UNAUTHENTICATED")
    void testPaymentWithMalformedJwtUnauthorized() throws Exception {
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 500.0, PaymentMethod.CASH);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer malformed.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("PASSENGER role can create simulated payment successfully")
    void testPassengerCanCreatePayment() throws Exception {
        String passengerToken = JwtTestHelper.generateToken("passenger-123", "PASSENGER");
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 860.0, PaymentMethod.CASH);
        PaymentResponse response = new PaymentResponse("payment-1", "ride-1", "fare-1", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("payment-1"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("ADMIN role can create simulated payment successfully")
    void testAdminCanCreatePayment() throws Exception {
        String adminToken = JwtTestHelper.generateToken("admin-123", "ADMIN");
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 860.0, PaymentMethod.CASH);
        PaymentResponse response = new PaymentResponse("payment-1", "ride-1", "fare-1", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));
    }

    @Test
    @DisplayName("DRIVER role can create payment because the contract requires authentication only")
    void testDriverCanCreatePayment() throws Exception {
        String driverToken = JwtTestHelper.generateToken("driver-123", "DRIVER");
        PaymentRequest request = new PaymentRequest("ride-1", "fare-1", 860.0, PaymentMethod.CASH);
        PaymentResponse response = new PaymentResponse("payment-1", "ride-1", "fare-1", 860.0,
                PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));
    }

    @Test
    @DisplayName("PASSENGER role can retrieve payment by ID")
    void testPassengerCanGetPaymentById() throws Exception {
        String passengerToken = JwtTestHelper.generateToken("passenger-123", "PASSENGER");
        PaymentResponse response = new PaymentResponse("payment-1", "ride-1", "fare-1", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getPaymentById("payment-1")).thenReturn(response);

        mockMvc.perform(get("/api/payments/payment-1")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));
    }

    @Test
    @DisplayName("DRIVER role can retrieve payment by ID because the contract requires authentication only")
    void testDriverCanGetPaymentById() throws Exception {
        String driverToken = JwtTestHelper.generateToken("driver-123", "DRIVER");
        PaymentResponse response = new PaymentResponse("payment-1", "ride-1", "fare-1", 860.0,
                PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getPaymentById("payment-1")).thenReturn(response);

        mockMvc.perform(get("/api/payments/payment-1")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));
    }

    @Test
    @DisplayName("PASSENGER role can retrieve receipt")
    void testPassengerCanGetReceipt() throws Exception {
        String passengerToken = JwtTestHelper.generateToken("passenger-123", "PASSENGER");
        ReceiptResponse receipt = new ReceiptResponse("payment-1", "ride-1", 10.0, 200.0, 50.0, 700.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getReceiptByPaymentId("payment-1")).thenReturn(receipt);

        mockMvc.perform(get("/api/payments/payment-1/receipt")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("payment-1"));
    }

    @Test
    @DisplayName("DRIVER role can retrieve receipt because the contract requires authentication only")
    void testDriverCanGetReceipt() throws Exception {
        String driverToken = JwtTestHelper.generateToken("driver-123", "DRIVER");
        ReceiptResponse receipt = new ReceiptResponse("payment-1", "ride-1", 10.0, 200.0, 50.0,
                700.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getReceiptByPaymentId("payment-1")).thenReturn(receipt);

        mockMvc.perform(get("/api/payments/payment-1/receipt")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("payment-1"));
    }

    @Test
    @DisplayName("Interservice fare estimation endpoint requires the forwarded JWT")
    void testFareEstimateRequiresJwt() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest("ride-1", 12.5);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Interservice fare estimation accepts a forwarded PASSENGER JWT")
    void testFareEstimateAcceptsPassengerJwt() throws Exception {
        String passengerToken = JwtTestHelper.generateToken("passenger-123", "PASSENGER");
        FareEstimateRequest request = new FareEstimateRequest("ride-1", 12.5);
        FareResponse response = new FareResponse("fare-1", "ride-1", "ESTIMATE", 12.5, 200.0, 50.0, 825.0);

        when(fareCalculationService.estimateFare(any(FareEstimateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fares/estimate")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareId").value("fare-1"))
                .andExpect(jsonPath("$.totalFare").value(825.0));
    }

    @Test
    @DisplayName("Interservice final fare endpoint requires the forwarded JWT")
    void testFinalFareRequiresJwt() throws Exception {
        FareFinalRequest request = new FareFinalRequest("ride-1", 13.2);

        mockMvc.perform(post("/api/fares/final")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Interservice final fare accepts a forwarded DRIVER JWT")
    void testFinalFareAcceptsDriverJwt() throws Exception {
        String driverToken = JwtTestHelper.generateToken("driver-123", "DRIVER");
        FareFinalRequest request = new FareFinalRequest("ride-1", 13.2);
        FareResponse response = new FareResponse("fare-2", "ride-1", "FINAL", 13.2, 200.0, 50.0, 860.0);

        when(fareCalculationService.calculateFinalFare(any(FareFinalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fares/final")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareId").value("fare-2"))
                .andExpect(jsonPath("$.totalFare").value(860.0));
    }

    @Test
    @DisplayName("PASSENGER role can retrieve payment by ride ID")
    void testPassengerCanGetPaymentByRideId() throws Exception {
        String passengerToken = JwtTestHelper.generateToken("passenger-123", "PASSENGER");
        PaymentResponse response = new PaymentResponse("payment-1", "ride-123", "fare-1", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getPaymentByRideId("ride-123")).thenReturn(response);

        mockMvc.perform(get("/api/payments/ride/ride-123")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-123"));
    }

    @Test
    @DisplayName("DRIVER role can retrieve payment by ride ID because the contract requires authentication only")
    void testDriverCanGetPaymentByRideId() throws Exception {
        String driverToken = JwtTestHelper.generateToken("driver-123", "DRIVER");
        PaymentResponse response = new PaymentResponse("payment-1", "ride-123", "fare-1", 860.0,
                PaymentMethod.CASH, PaymentStatus.PAID, Instant.now());

        when(paymentService.getPaymentByRideId("ride-123")).thenReturn(response);

        mockMvc.perform(get("/api/payments/ride/ride-123")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-123"));
    }

    @Test
    @DisplayName("Swagger and OpenAPI documentation endpoints are permitted without authentication by security filter")
    void testSwaggerEndpointsAccessibleWithoutJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }
}
