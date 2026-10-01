package com.ridelink.payment.service;

import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.Payment;
import com.ridelink.payment.domain.PaymentStatus;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;
import com.ridelink.payment.exception.ConflictException;
import com.ridelink.payment.exception.InvalidPaymentStateException;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.repository.FareRepository;
import com.ridelink.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, FareRepository fareRepository) {
        this.paymentRepository = paymentRepository;
        this.fareRepository = fareRepository;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        validatePaymentRequest(request);

        // Check if a completed (PAID) payment already exists for this ride
        Optional<Payment> existingPayment = paymentRepository.findByRideId(request.getRideId());
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Payment already completed for ride: " + request.getRideId());
        }

        // Validate that the fare exists
        fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with id: " + request.getFareId()));

        BigDecimal paymentAmount = BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_UP);

        Payment payment = new Payment();
        payment.setRideId(request.getRideId());
        payment.setFareId(request.getFareId());
        payment.setAmount(paymentAmount.doubleValue());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus(PaymentStatus.PAID);
        Instant now = Instant.now();
        payment.setPaidAt(now);
        payment.setCreatedAt(now);

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    public PaymentResponse createFailedPayment(PaymentRequest request) {
        validatePaymentRequest(request);

        fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with id: " + request.getFareId()));

        BigDecimal paymentAmount = BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_UP);

        Payment payment = new Payment();
        payment.setRideId(request.getRideId());
        payment.setFareId(request.getFareId());
        payment.setAmount(paymentAmount.doubleValue());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaidAt(null);
        payment.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    public PaymentResponse getPaymentByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride id: " + rideId));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    public ReceiptResponse getReceiptByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            throw new InvalidPaymentStateException("Receipt is only available for completed (PAID) payments");
        }

        Fare fare = fareRepository.findById(payment.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare not found with id: " + payment.getFareId()));

        return ReceiptResponse.from(payment, fare);
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (request.getRideId() == null || request.getRideId().isBlank()) {
            throw new IllegalArgumentException("rideId is required");
        }
        if (request.getFareId() == null || request.getFareId().isBlank()) {
            throw new IllegalArgumentException("fareId is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("paymentMethod is required");
        }
    }
}
