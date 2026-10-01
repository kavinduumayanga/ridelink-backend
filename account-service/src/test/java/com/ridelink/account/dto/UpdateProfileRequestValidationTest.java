package com.ridelink.account.dto;

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

class UpdateProfileRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid profile fields")
    void testValidUpdateProfileRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest("Kavindu", "Umayanga", "+94779999999");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should fail validation on blank profile fields")
    void testBlankProfileFields() {
        UpdateProfileRequest request = new UpdateProfileRequest("", "", "");
        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 3);
    }
}
