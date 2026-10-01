package com.ridelink.account.dto;

import com.ridelink.account.domain.AccountStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStatusRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid status")
    void testValidUpdateStatusRequest() {
        UpdateStatusRequest request = new UpdateStatusRequest(AccountStatus.INACTIVE);
        Set<ConstraintViolation<UpdateStatusRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should fail validation on null status")
    void testNullStatus() {
        UpdateStatusRequest request = new UpdateStatusRequest(null);
        Set<ConstraintViolation<UpdateStatusRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }
}
