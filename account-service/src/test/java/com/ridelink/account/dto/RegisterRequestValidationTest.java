package com.ridelink.account.dto;

import com.ridelink.account.domain.Role;
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

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid request")
    void testValidRequest() {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should fail validation when required fields are blank")
    void testBlankRequiredFields() {
        RegisterRequest request = new RegisterRequest(
                "",
                "",
                "",
                "",
                "",
                null
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 5);
    }

    @Test
    @DisplayName("Should fail validation on invalid email format")
    void testInvalidEmail() {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "not-an-email",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Should fail validation on password shorter than 8 characters")
    void testShortPassword() {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "short",
                Role.PASSENGER
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should fail validation on null role")
    void testNullRole() {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "secureP@ss1",
                null
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("role")));
    }
}
