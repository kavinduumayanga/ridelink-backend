package com.ridelink.payment.dto;

import com.ridelink.payment.domain.Payment;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response DTO representing payment details and status")
public class PaymentResponse {

    @Schema(description = "MongoDB payment ID", example = "cc7f6a7b8c9d0e1f2a3b4c5d")
    private String paymentId;

    @Schema(description = "MongoDB ride ID", example = "995c3d4e5f6a7b8c9d0e1f2a")
    private String rideId;

    @Schema(description = "MongoDB fare ID", example = "bb6e5f6a7b8c9d0e1f2a3b4c")
    private String fareId;

    @Schema(description = "Payment amount in LKR", example = "860.00")
    private Double amount;

    @Schema(description = "Payment method used", example = "CASH")
    private PaymentMethod paymentMethod;

    @Schema(description = "Current payment status (PAID, PENDING, FAILED)", example = "PAID")
    private PaymentStatus paymentStatus;

    @Schema(description = "Timestamp when payment was completed", example = "2026-09-27T17:30:00.000+00:00")
    private Instant paidAt;

    public PaymentResponse() {
    }

    public PaymentResponse(String paymentId, String rideId, String fareId, Double amount, PaymentMethod paymentMethod, PaymentStatus paymentStatus, Instant paidAt) {
        this.paymentId = paymentId;
        this.rideId = rideId;
        this.fareId = fareId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
    }

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getRideId(),
                payment.getFareId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaidAt()
        );
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getFareId() {
        return fareId;
    }

    public void setFareId(String fareId) {
        this.fareId = fareId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
