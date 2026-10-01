package com.ridelink.payment.dto;

import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.Payment;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response DTO representing combined ride receipt with fare and payment breakdown")
public class ReceiptResponse {

    @Schema(description = "Receipt ID (matches Payment ID)", example = "cc7f6a7b8c9d0e1f2a3b4c5d")
    private String receiptId;

    @Schema(description = "MongoDB ride ID", example = "995c3d4e5f6a7b8c9d0e1f2a")
    private String rideId;

    @Schema(description = "Actual ride distance in kilometers", example = "13.2")
    private Double distanceKm;

    @Schema(description = "Base fare in LKR", example = "200.00")
    private Double baseFare;

    @Schema(description = "Rate per kilometer in LKR", example = "50.00")
    private Double ratePerKm;

    @Schema(description = "Total fare charged in LKR", example = "860.00")
    private Double totalFare;

    @Schema(description = "Payment method used", example = "CASH")
    private PaymentMethod paymentMethod;

    @Schema(description = "Payment status (PAID)", example = "PAID")
    private PaymentStatus paymentStatus;

    @Schema(description = "Timestamp when payment was processed", example = "2026-09-27T17:30:00.000+00:00")
    private Instant paidAt;

    public ReceiptResponse() {
    }

    public ReceiptResponse(String receiptId, String rideId, Double distanceKm, Double baseFare, Double ratePerKm, Double totalFare, PaymentMethod paymentMethod, PaymentStatus paymentStatus, Instant paidAt) {
        this.receiptId = receiptId;
        this.rideId = rideId;
        this.distanceKm = distanceKm;
        this.baseFare = baseFare;
        this.ratePerKm = ratePerKm;
        this.totalFare = totalFare;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
    }

    public static ReceiptResponse from(Payment payment, Fare fare) {
        return new ReceiptResponse(
                payment.getId(),
                payment.getRideId(),
                fare != null ? fare.getDistanceKm() : null,
                fare != null ? fare.getBaseFare() : null,
                fare != null ? fare.getRatePerKm() : null,
                fare != null ? fare.getTotalFare() : null,
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaidAt()
        );
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(Double baseFare) {
        this.baseFare = baseFare;
    }

    public Double getRatePerKm() {
        return ratePerKm;
    }

    public void setRatePerKm(Double ratePerKm) {
        this.ratePerKm = ratePerKm;
    }

    public Double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(Double totalFare) {
        this.totalFare = totalFare;
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
