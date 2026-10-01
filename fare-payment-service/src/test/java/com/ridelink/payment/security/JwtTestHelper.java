package com.ridelink.payment.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTestHelper {

    private static final String DEFAULT_SECRET = "dGhpcy1pcy1hLXNhZmUtZGV2ZWxvcG1lbnQtdGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItcmlkZWxpbms=";

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(String userId, String role) {
        return generateToken(userId, role, DEFAULT_SECRET, 3600_000L);
    }

    public static String generateToken(String userId, String role, String secret, long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(secret))
                .compact();
    }

    public static String generateExpiredToken(String userId, String role) {
        Date now = new Date(System.currentTimeMillis() - 10000);
        Date expiry = new Date(System.currentTimeMillis() - 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(DEFAULT_SECRET))
                .compact();
    }
}
