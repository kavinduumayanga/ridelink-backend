package com.ridelink.payment.service;

import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.FareType;
import com.ridelink.payment.domain.Payment;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.InvalidPaymentStateException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.repository.FareRepository;
import com.ridelink.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, fareRepository);
    }

    @Test
    @DisplayName("Should successfully create simulated CASH payment with PAID status")
    void testCreatePaymentCashSuccess() {
        PaymentRequest request = new PaymentRequest("ride-123", "fare-456", 860.00, PaymentMethod.CASH);
        Fare mockFare = new Fare("fare-456", "ride-123", FareType.FINAL, 13.2, 200.0, 50.0, 860.0, Instant.now());

        when(paymentRepository.findByRideId("ride-123")).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-456")).thenReturn(Optional.of(mockFare));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("payment-789");
            return p;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("payment-789", response.getPaymentId());
        assertEquals("ride-123", response.getRideId());
        assertEquals("fare-456", response.getFareId());
        assertEquals(860.00, response.getAmount());
        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertNotNull(response.getPaidAt());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        Payment saved = captor.getValue();
        assertEquals(PaymentStatus.PAID, saved.getPaymentStatus());
        assertNotNull(saved.getPaidAt());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Should successfully create simulated CARD_SIMULATED payment with PAID status")
    void testCreatePaymentCardSimulatedSuccess() {
        PaymentRequest request = new PaymentRequest("ride-123", "fare-456", 500.00, PaymentMethod.CARD_SIMULATED);
        Fare mockFare = new Fare("fare-456", "ride-123", FareType.FINAL, 6.0, 200.0, 50.0, 500.0, Instant.now());

        when(paymentRepository.findByRideId("ride-123")).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-456")).thenReturn(Optional.of(mockFare));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("payment-999");
            return p;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("payment-999", response.getPaymentId());
        assertEquals(PaymentMethod.CARD_SIMULATED, response.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertNotNull(response.getPaidAt());
    }

    @Test
    @DisplayName("Should prevent duplicate payment creation if ride already has PAID payment")
    void testCreatePaymentDuplicateConflict() {
        PaymentRequest request = new PaymentRequest("ride-123", "fare-456", 860.00, PaymentMethod.CASH);
        Payment existingPayment = new Payment("existing-payment-1", "ride-123", "fare-456", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, Instant.now(), Instant.now());

        when(paymentRepository.findByRideId("ride-123")).thenReturn(Optional.of(existingPayment));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                paymentService.createPayment(request)
        );
        assertTrue(ex.getMessage().contains("Payment already completed"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fare does not exist")
    void testCreatePaymentFareNotFound() {
        PaymentRequest request = new PaymentRequest("ride-123", "fare-not-found", 860.00, PaymentMethod.CASH);

        when(paymentRepository.findByRideId("ride-123")).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-not-found")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.createPayment(request)
        );
    }

    @Test
    @DisplayName("Should support simulated failed payment with FAILED status and null paidAt")
    void testCreateFailedPayment() {
        PaymentRequest request = new PaymentRequest("ride-123", "fare-456", 860.00, PaymentMethod.CARD_SIMULATED);
        Fare mockFare = new Fare("fare-456", "ride-123", FareType.FINAL, 13.2, 200.0, 50.0, 860.0, Instant.now());

        when(fareRepository.findById("fare-456")).thenReturn(Optional.of(mockFare));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("failed-payment-123");
            return p;
        });

        PaymentResponse response = paymentService.createFailedPayment(request);

        assertNotNull(response);
        assertEquals("failed-payment-123", response.getPaymentId());
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertNull(response.getPaidAt());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentStatus.FAILED, captor.getValue().getPaymentStatus());
        assertNull(captor.getValue().getPaidAt());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on invalid input")
    void testCreatePaymentInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(new PaymentRequest("", "fare-1", 100.0, PaymentMethod.CASH))
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(new PaymentRequest("ride-1", "", 100.0, PaymentMethod.CASH))
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(new PaymentRequest("ride-1", "fare-1", -10.0, PaymentMethod.CASH))
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(new PaymentRequest("ride-1", "fare-1", 100.0, null))
        );
    }

    @Test
    @DisplayName("Should retrieve payment by paymentId")
    void testGetPaymentByIdSuccess() {
        Instant paidTime = Instant.now();
        Payment payment = new Payment("payment-123", "ride-123", "fare-456", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, paidTime, paidTime);

        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById("payment-123");

        assertNotNull(response);
        assertEquals("payment-123", response.getPaymentId());
        assertEquals("ride-123", response.getRideId());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when paymentId not found")
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentById("non-existent")
        );
    }

    @Test
    @DisplayName("Should retrieve payment by rideId")
    void testGetPaymentByRideIdSuccess() {
        Instant paidTime = Instant.now();
        Payment payment = new Payment("payment-123", "ride-123", "fare-456", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, paidTime, paidTime);

        when(paymentRepository.findByRideId("ride-123")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByRideId("ride-123");

        assertNotNull(response);
        assertEquals("payment-123", response.getPaymentId());
        assertEquals("ride-123", response.getRideId());
    }

    @Test
    @DisplayName("Should retrieve receipt for valid PAID payment")
    void testGetReceiptSuccess() {
        Instant paidTime = Instant.now();
        Payment payment = new Payment("payment-123", "ride-123", "fare-456", 860.0, PaymentMethod.CASH, PaymentStatus.PAID, paidTime, paidTime);
        Fare fare = new Fare("fare-456", "ride-123", FareType.FINAL, 13.2, 200.0, 50.0, 860.0, paidTime);

        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));
        when(fareRepository.findById("fare-456")).thenReturn(Optional.of(fare));

        ReceiptResponse receipt = paymentService.getReceiptByPaymentId("payment-123");

        assertNotNull(receipt);
        assertEquals("payment-123", receipt.getReceiptId());
        assertEquals("ride-123", receipt.getRideId());
        assertEquals(13.2, receipt.getDistanceKm());
        assertEquals(200.00, receipt.getBaseFare());
        assertEquals(50.00, receipt.getRatePerKm());
        assertEquals(860.00, receipt.getTotalFare());
        assertEquals(PaymentMethod.CASH, receipt.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, receipt.getPaymentStatus());
        assertEquals(paidTime, receipt.getPaidAt());
    }

    @Test
    @DisplayName("Should reject receipt generation for FAILED payment")
    void testGetReceiptForFailedPaymentThrowsException() {
        Payment failedPayment = new Payment("payment-123", "ride-123", "fare-456", 860.0, PaymentMethod.CARD_SIMULATED, PaymentStatus.FAILED, null, Instant.now());

        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(failedPayment));

        assertThrows(InvalidPaymentStateException.class, () ->
                paymentService.getReceiptByPaymentId("payment-123")
        );
    }
}
