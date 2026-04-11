package com.treinamento.ctf.middleware;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET = "super-secret-jwt-key-2024";
    private static final long EXPIRATION_MS = 86_400_000L; // 24 hours

    private Key getSigningKey() {
        byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Integer userId, String email, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of(
                        "email", email,
                        "role", role
                ))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            Claims claims = parseToken(token);
            return (String) claims.get("role");
        } catch (Exception e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        try {
            Claims claims = parseToken(token);
            return (String) claims.get("email");
        } catch (Exception e) {
            return null;
        }
    }
}
