package com.example.common.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 身份编解码器：在身份头、JWT claim、{@link UserIdentity} 三者之间相互转换。
 *
 * <p>头名与 claim 名统一取自 {@link AuthHeaders}，网关、下游服务、Feign 都复用
 * 这一套逻辑，不再各自硬编码。</p>
 */
public final class UserIdentityCodec {

    /** 存入 RequestAttributes 时使用的键，供跨线程（如 CircuitBreaker）传播使用。 */
    public static final String REQUEST_ATTRIBUTE = "userIdentity";

    private UserIdentityCodec() {
    }

    /**
     * 从 Servlet 请求头解析用户身份。
     *
     * @param request 当前请求
     * @return 身份模型；未携带账号标识时返回 {@link UserIdentity#EMPTY}
     */
    public static UserIdentity read(HttpServletRequest request) {
        return new UserIdentity(
                parseLong(request.getHeader(AuthHeaders.ACCOUNT_ID.header())),
                request.getHeader(AuthHeaders.DEVICE_ID.header()),
                request.getHeader(AuthHeaders.APP_KEY.header()),
                parseRoles(request.getHeader(AuthHeaders.ROLES.header())));
    }

    /** 从 JWT claim 解析用户身份。 */
    public static UserIdentity fromClaims(Claims claims) {
        return new UserIdentity(
                parseLong(claims.get(AuthHeaders.ACCOUNT_ID.claim())),
                stringOf(claims.get(AuthHeaders.DEVICE_ID.claim())),
                stringOf(claims.get(AuthHeaders.APP_KEY.claim())),
                parseRoles(claims.get(AuthHeaders.ROLES.claim())));
    }

    /** 将用户身份转换为 JWT claim 集合，供签发 token 使用。 */
    public static Map<String, Object> toClaims(UserIdentity identity) {
        Map<String, Object> claims = new LinkedHashMap<>();
        if (identity == null) {
            return claims;
        }
        if (identity.userId() != null) {
            claims.put(AuthHeaders.ACCOUNT_ID.claim(), String.valueOf(identity.userId()));
        }
        if (identity.deviceId() != null) {
            claims.put(AuthHeaders.DEVICE_ID.claim(), identity.deviceId());
        }
        if (identity.appKey() != null) {
            claims.put(AuthHeaders.APP_KEY.claim(), identity.appKey());
        }
        if (!identity.roles().isEmpty()) {
            claims.put(AuthHeaders.ROLES.claim(), identity.roles());
        }
        return claims;
    }

    /** 将用户身份转换为内部身份头集合，供网关/Feign 透传使用。 */
    public static Map<String, String> toHeaders(UserIdentity identity) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (identity == null) {
            return headers;
        }
        if (identity.userId() != null) {
            headers.put(AuthHeaders.ACCOUNT_ID.header(), String.valueOf(identity.userId()));
        }
        if (identity.deviceId() != null) {
            headers.put(AuthHeaders.DEVICE_ID.header(), identity.deviceId());
        }
        if (identity.appKey() != null) {
            headers.put(AuthHeaders.APP_KEY.header(), identity.appKey());
        }
        if (!identity.roles().isEmpty()) {
            headers.put(AuthHeaders.ROLES.header(), String.join(",", identity.roles()));
        }
        return headers;
    }

    private static String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> parseRoles(Object roles) {
        if (roles == null) {
            return List.of();
        }
        if (roles instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .toList();
        }
        return parseRoles(String.valueOf(roles));
    }

    private static List<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
