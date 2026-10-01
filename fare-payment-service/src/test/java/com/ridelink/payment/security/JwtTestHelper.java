package com.ridelink.payment.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTestHelper {

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(String userId, String role, String secret) {
        return generateToken(userId, role, secret, 3600_000L);
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

    public static String generateExpiredToken(String userId, String role, String secret) {
        Date now = new Date(System.currentTimeMillis() - 10000);
        Date expiry = new Date(System.currentTimeMillis() - 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(secret))
                .compact();
    }
}
