package com.ridelink.driver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Validates JWTs issued by Account Service.
 * Driver Service does NOT issue tokens — it only verifies them.
 */
@Component
public class JwtTokenProvider {

    private final Key signingKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Validates the JWT signature and expiration, then returns the parsed claims.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return parsed Claims if the token is valid
     * @throws JwtException if the token is invalid, expired, or malformed
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the userId (sub claim) from validated claims.
     */
    public String getUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extracts the role claim from validated claims.
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
