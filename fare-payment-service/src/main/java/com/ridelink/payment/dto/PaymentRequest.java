package com.ridelink.payment.dto;

import com.ridelink.payment.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for creating a simulated payment")
public class PaymentRequest {

    @NotBlank(message = "rideId is required")
    @Schema(description = "MongoDB ride ID", example = "995c3d4e5f6a7b8c9d0e1f2a", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rideId;

    @NotBlank(message = "fareId is required")
    @Schema(description = "MongoDB fare ID", example = "bb6e5f6a7b8c9d0e1f2a3b4c", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fareId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    @Schema(description = "Payment amount in LKR; must be greater than 0", example = "860.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double amount;

    @NotNull(message = "paymentMethod is required")
    @Schema(description = "Payment method (CASH or CARD_SIMULATED)", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod paymentMethod;

    @Schema(description = "Deterministically simulate a failed payment; defaults to false", example = "false", defaultValue = "false")
    private boolean simulateFailure;

    public PaymentRequest() {
    }

    public PaymentRequest(String rideId, String fareId, Double amount, PaymentMethod paymentMethod) {
        this.rideId = rideId;
        this.fareId = fareId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public PaymentRequest(String rideId, String fareId, Double amount, PaymentMethod paymentMethod, boolean simulateFailure) {
        this(rideId, fareId, amount, paymentMethod);
        this.simulateFailure = simulateFailure;
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

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
