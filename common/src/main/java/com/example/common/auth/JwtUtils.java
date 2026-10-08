package com.example.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
}
