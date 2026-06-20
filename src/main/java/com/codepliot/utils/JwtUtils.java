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

/**
 * JWT 令牌工具类。
 * <p>
 * 提供 JWT 令牌的生成、解析、验证等功能，用于用户登录认证和 GitHub OAuth 状态令牌管理。
 * 基于 HMAC-SHA 签名算法保证令牌的完整性和安全性。
 * </p>
 */
@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;

    /**
     * 构造方法，注入 JWT 配置属性。
     *
     * @param jwtProperties JWT 配置属性，包含签名密钥和过期时间等配置
     */
    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 为登录用户生成 JWT 令牌。
     * <p>
     * 令牌中包含用户名（subject）、用户ID、邮箱等声明信息，
     * 并设置签发时间和过期时间。
     * </p>
     *
     * @param loginUser 登录用户信息对象
     * @return 签名后的 JWT 令牌字符串
     */
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

    /**
     * 从 JWT 令牌中提取用户名。
     *
     * @param token JWT 令牌字符串
     * @return 令牌中存储的用户名
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 从 JWT 令牌中提取用户ID。
     * <p>
     * 兼容 Integer 和 Long 两种类型，确保返回 Long 类型。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌中存储的用户ID
     */
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

    /**
     * 验证 JWT 令牌是否有效。
     * <p>
     * 检查令牌签名是否正确且未过期。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌有效返回 true，否则返回 false
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 生成 GitHub OAuth 状态令牌。
     * <p>
     * 用于防止 CSRF 攻击，在 GitHub OAuth 授权流程中携带用户ID信息。
     * </p>
     *
     * @param userId       当前用户ID
     * @param expireSeconds 令牌过期时间（秒）
     * @return 签名后的 GitHub OAuth 状态令牌字符串
     */
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

    /**
     * 验证 GitHub OAuth 状态令牌。
     * <p>
     * 检查令牌的 subject 是否为 "github_oauth_state"，并验证用户ID是否匹配预期值。
     * </p>
     *
     * @param token         GitHub OAuth 状态令牌字符串
     * @param expectedUserId 预期的用户ID
     * @throws IllegalArgumentException 当令牌无效或用户ID不匹配时抛出
     */
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

    /**
     * 解析 JWT 令牌并返回声明信息。
     *
     * @param token JWT 令牌字符串
     * @return 令牌中的声明信息（Claims）
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 根据配置的密钥字符串生成 HMAC-SHA 签名密钥。
     *
     * @return HMAC-SHA 签名密钥对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
