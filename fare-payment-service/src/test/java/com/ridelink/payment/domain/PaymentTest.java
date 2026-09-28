package com.ridelink.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    @DisplayName("Should create payment with all arguments and getters match")
    void testPaymentCreation() {
        Instant now = Instant.now();
        Instant paidAt = now.plusSeconds(5);
        Payment payment = new Payment(
                "payment-123",
                "ride-456",
                "fare-789",
                860.00,
                PaymentMethod.CASH,
                PaymentStatus.PAID,
                paidAt,
                now
        );

        assertEquals("payment-123", payment.getId());
        assertEquals("ride-456", payment.getRideId());
        assertEquals("fare-789", payment.getFareId());
        assertEquals(860.00, payment.getAmount());
        assertEquals(PaymentMethod.CASH, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals(paidAt, payment.getPaidAt());
        assertEquals(now, payment.getCreatedAt());
    }

    @Test
    @DisplayName("Should support setters and no-arg constructor")
    void testPaymentSetters() {
        Payment payment = new Payment();
        payment.setId("payment-001");
        payment.setRideId("ride-001");
        payment.setFareId("fare-001");
        payment.setAmount(500.00);
        payment.setPaymentMethod(PaymentMethod.CARD_SIMULATED);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        Instant now = Instant.now();
        payment.setCreatedAt(now);
        payment.setPaidAt(null);

        assertEquals("payment-001", payment.getId());
        assertEquals("ride-001", payment.getRideId());
        assertEquals("fare-001", payment.getFareId());
        assertEquals(500.00, payment.getAmount());
        assertEquals(PaymentMethod.CARD_SIMULATED, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertNull(payment.getPaidAt());
        assertEquals(now, payment.getCreatedAt());
    }

    @Test
    @DisplayName("Should verify PaymentStatus enum values")
    void testPaymentStatusValues() {
        assertEquals(3, PaymentStatus.values().length);
        assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
        assertEquals(PaymentStatus.PAID, PaymentStatus.valueOf("PAID"));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
    }

    @Test
    @DisplayName("Should obey equals and hashCode based on ID")
    void testPaymentEqualsAndHashCode() {
        Payment payment1 = new Payment("payment-123", "ride-1", "fare-1", 100.0, PaymentMethod.CASH, PaymentStatus.PAID, null, Instant.now());
        Payment payment2 = new Payment("payment-123", "ride-2", "fare-2", 200.0, PaymentMethod.CARD_SIMULATED, PaymentStatus.PENDING, null, Instant.now());
        Payment payment3 = new Payment("payment-456", "ride-1", "fare-1", 100.0, PaymentMethod.CASH, PaymentStatus.PAID, null, Instant.now());

        assertEquals(payment1, payment2);
        assertEquals(fareHashCode(payment1), fareHashCode(payment2));
        assertNotEquals(payment1, payment3);
    }

    private int fareHashCode(Payment p) {
        return p.hashCode();
    }
}
