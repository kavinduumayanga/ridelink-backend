package com.ridelink.account.security;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "dGVzdFNlY3JldEtleUZvclJpZGVMaW5rSnd0U2lnbmluZ011c3RCZUF0TGVhc3QyNTZCaXRzTG9uZw==";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(testSecret, expirationMs);
    }

    @Test
    @DisplayName("Should generate JWT with correct sub, role, and expiration claims")
    void testGenerateTokenWithClaims() {
        User user = new User(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "hashedPass",
                "+94771234567",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );

        String token = jwtTokenProvider.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.getClaims(token);
        assertEquals("665f1a2b3c4d5e6f7a8b9c0d", claims.getSubject());
        assertEquals("PASSENGER", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    @DisplayName("Should extract userId and role from token")
    void testExtractUserIdAndRole() {
        String token = jwtTokenProvider.generateToken("775a1b2c3d4e5f6a7b8c9d0e", Role.DRIVER);

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        Role role = jwtTokenProvider.getRoleFromToken(token);

        assertEquals("775a1b2c3d4e5f6a7b8c9d0e", userId);
        assertEquals(Role.DRIVER, role);
    }

    @Test
    @DisplayName("Should validate valid token and reject invalid/tampered token")
    void testValidateToken() {
        String token = jwtTokenProvider.generateToken("user123", Role.ADMIN);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertFalse(jwtTokenProvider.validateToken(token + "tampered"));
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.string"));
    }

    @Test
    @DisplayName("Should respect configuration-driven expiration and secret")
    void testConfigurationDrivenParameters() {
        JwtTokenProvider customProvider = new JwtTokenProvider("anotherSecretKeyThatIsSufficientlyLongForHS256Algorithm12345", 5000);
        assertEquals(5000, customProvider.getExpirationMs());

        String token = customProvider.generateToken("user456", Role.PASSENGER);
        Claims claims = customProvider.getClaims(token);
        assertEquals("user456", claims.getSubject());
    }
}
