package com.ridelink.account.dto;

import com.ridelink.account.domain.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Account status update request payload")
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target account status (ACTIVE or INACTIVE)", example = "INACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
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
