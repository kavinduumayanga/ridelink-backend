package com.ridelink.payment.service;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.dto.PaymentResponse;
import com.ridelink.payment.dto.ReceiptResponse;

public interface PaymentService {

    PaymentResponse createPayment(PaymentRequest request);

    PaymentResponse createFailedPayment(PaymentRequest request);

    PaymentResponse getPaymentById(String paymentId);

    PaymentResponse getPaymentByRideId(String rideId);

    ReceiptResponse getReceiptByPaymentId(String paymentId);
}
