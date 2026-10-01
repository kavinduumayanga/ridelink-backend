package com.ridelink.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET = "dGhpcy1pcy1hLXNhZmUtZGV2ZWxvcG1lbnQtdGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItcmlkZWxpbms=";
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET);
    }

    @Test
    @DisplayName("Should validate valid token and extract userId and role")
    void testValidToken() {
        String token = JwtTestHelper.generateToken("user-123", "PASSENGER");

        assertTrue(jwtUtils.validateToken(token));
        assertEquals("user-123", jwtUtils.getUserId(token));
        assertEquals("PASSENGER", jwtUtils.getRole(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredToken() {
        String expiredToken = JwtTestHelper.generateExpiredToken("user-123", "PASSENGER");

        assertFalse(jwtUtils.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testInvalidSignature() {
        String invalidSecret = "dGhpcy1pcy1hLWRpZmZlcmVudC1zZWNyZXQta2V5LWZvci10ZXN0aW5nMTIzNDU2Nzg5MA==";
        String token = JwtTestHelper.generateToken("user-123", "PASSENGER", invalidSecret, 3600_000L);

        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("Should reject malformed or null tokens")
    void testMalformedOrNullToken() {
        assertFalse(jwtUtils.validateToken(null));
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken("not.a.valid.jwt.token"));
    }
}
