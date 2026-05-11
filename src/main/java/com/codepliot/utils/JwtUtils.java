package com.codepliot.utils;

import com.codepliot.config.JwtProperties;
import com.codepliot.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claims(Map.of(
                        "userId", loginUser.getUserId(),
                        "email", loginUser.getEmail()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (userId instanceof Long longValue) {
            return longValue;
        }
        return Long.valueOf(String.valueOf(userId));
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    public String generateGitHubOauthStateToken(Long userId, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("github_oauth_state")
                .claims(Map.of("userId", userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(getSecretKey())
                .compact();
    }

    public void validateGitHubOauthStateToken(String token, Long expectedUserId) {
        Claims claims = parseClaims(token);
        if (!"github_oauth_state".equals(claims.getSubject())) {
            throw new IllegalArgumentException("Invalid GitHub OAuth state");
        }
        Object userId = claims.get("userId");
        Long actualUserId;
        if (userId instanceof Integer integerValue) {
            actualUserId = integerValue.longValue();
        } else if (userId instanceof Long longValue) {
            actualUserId = longValue;
        } else {
            actualUserId = Long.valueOf(String.valueOf(userId));
        }
        if (!expectedUserId.equals(actualUserId)) {
            throw new IllegalArgumentException("GitHub OAuth state does not match current user");
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
