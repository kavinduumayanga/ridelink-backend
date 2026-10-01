package com.ridelink.driver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider — validates token parsing, claim extraction,
 * and error handling for invalid/expired tokens.
 */
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "TestSecretKeyThatIsAtLeast32BytesLong!!";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
    }

    @Test
    void testValidToken_ExtractsClaimsSuccessfully() {
        String token = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "DRIVER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);

        assertNotNull(claims);
        assertEquals("user-123", jwtTokenProvider.getUserId(claims));
        assertEquals("DRIVER", jwtTokenProvider.getRole(claims));
    }

    @Test
    void testValidToken_PassengerRole() {
        String token = Jwts.builder()
                .setSubject("user-456")
                .claim("role", "PASSENGER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);

        assertEquals("user-456", jwtTokenProvider.getUserId(claims));
        assertEquals("PASSENGER", jwtTokenProvider.getRole(claims));
    }

    @Test
    void testValidToken_AdminRole() {
        String token = Jwts.builder()
                .setSubject("admin-789")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);

        assertEquals("admin-789", jwtTokenProvider.getUserId(claims));
        assertEquals("ADMIN", jwtTokenProvider.getRole(claims));
    }

    @Test
    void testExpiredToken_ThrowsJwtException() {
        String token = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "DRIVER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(JwtException.class, () -> jwtTokenProvider.validateAndGetClaims(token));
    }

    @Test
    void testInvalidSignature_ThrowsJwtException() {
        Key wrongKey = Keys.hmacShaKeyFor("DifferentSecretKeyThatIsAtLeast32Bytes!!".getBytes());
        String token = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "DRIVER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(JwtException.class, () -> jwtTokenProvider.validateAndGetClaims(token));
    }

    @Test
    void testMalformedToken_ThrowsJwtException() {
        assertThrows(JwtException.class, () -> jwtTokenProvider.validateAndGetClaims("not.a.valid.jwt"));
    }

    @Test
    void testTokenWithoutRoleClaim_ReturnsNullRole() {
        String token = Jwts.builder()
                .setSubject("user-123")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);

        assertEquals("user-123", jwtTokenProvider.getUserId(claims));
        assertNull(jwtTokenProvider.getRole(claims));
    }
}
