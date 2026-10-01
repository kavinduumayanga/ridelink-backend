package com.ridelink.account.dto;

import com.ridelink.account.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;

    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(AccountStatus status) {
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
