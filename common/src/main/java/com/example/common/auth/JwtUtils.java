package com.example.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT 签发与解析工具。
 *
 * <p>签发时按 {@link AuthHeaders} 约定的 claim 名自动填入 {@link UserIdentity}，
 * 解析与校验逻辑与网关共用，避免签发方与网关各写一套。</p>
 */
public final class JwtUtils {

    private JwtUtils() {
    }

    /** 根据密钥字符串生成 HS256 签名密钥。 */
    public static SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 token（无 issuer）。
     *
     * @param secret    签名密钥字符串
     * @param identity  需要写入的身份信息
     * @param ttlMillis 有效期（毫秒）
     */
    public static String createToken(String secret, UserIdentity identity, long ttlMillis) {
        return createToken(secret, identity, ttlMillis, null);
    }

    /**
     * 签发 token。
     *
     * @param secret    签名密钥字符串
     * @param identity  需要写入的身份信息
     * @param ttlMillis 有效期（毫秒）
     * @param issuer    签发者（可为 null）
     */
    public static String createToken(String secret, UserIdentity identity, long ttlMillis, String issuer) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .claims(UserIdentityCodec.toClaims(identity))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis));
        if (issuer != null && !issuer.isBlank()) {
            builder.issuer(issuer);
        }
        return builder.signWith(secretKey(secret)).compact();
    }

    /** 解析并校验 token（签名 + 过期），失败抛出 {@link io.jsonwebtoken.JwtException}。 */
    public static Claims parseToken(String secret, String token) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 签发长 Token（Refresh Token）。
     *
     * <p>长 Token 仅用于「定位会话 + 防重放」，不作为权限凭证：只写入
     * {@code account_id} / {@code device_id} / {@code jti}，不写入 roles / app_key。</p>
     *
     * @param secret    长 Token 签名密钥（与短 Token 密钥隔离）
     * @param userId    用户 ID
     * @param deviceId  设备指纹（可为空）
     * @param jti       本次长 Token 的 ID（轮换时更新）
     * @param ttlMillis 有效期（毫秒）
     * @param issuer    签发者（可为 null）
     */
    public static String createRefreshToken(String secret, Long userId, String deviceId,
                                            String jti, long ttlMillis, String issuer) {
        Date now = new Date();
        Map<String, Object> claims = new LinkedHashMap<>();
        if (userId != null) {
            claims.put(AuthHeaders.ACCOUNT_ID.claim(), String.valueOf(userId));
        }
        if (deviceId != null) {
            claims.put(AuthHeaders.DEVICE_ID.claim(), deviceId);
        }
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .id(jti)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis));
        if (issuer != null && !issuer.isBlank()) {
            builder.issuer(issuer);
        }
        return builder.signWith(secretKey(secret)).compact();
    }

    /**
     * 解析并校验长 Token，提取 userId / deviceId / jti，失败抛出 {@link io.jsonwebtoken.JwtException}。
     */
    public static RefreshTokenInfo parseRefreshToken(String secret, String token) {
        Claims claims = parseToken(secret, token);
        Long userId = toLong(claims.get(AuthHeaders.ACCOUNT_ID.claim()));
        String deviceId = toStr(claims.get(AuthHeaders.DEVICE_ID.claim()));
        return new RefreshTokenInfo(userId, deviceId, claims.getId());
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
