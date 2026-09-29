package com.ridelink.payment.dto;

import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.Payment;
import com.ridelink.payment.domain.PaymentMethod;
import com.ridelink.payment.domain.PaymentStatus;

import java.time.Instant;

public class ReceiptResponse {

    private String receiptId;
    private String rideId;
    private Double distanceKm;
    private Double baseFare;
    private Double ratePerKm;
    private Double totalFare;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
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
