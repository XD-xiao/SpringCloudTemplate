package com.example.gateway.filters;

import com.example.gateway.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /** JWT 签名密钥（HS256，需与签发方一致） */
    @Value("${jwt.secret:change-me-to-a-32-bytes-secret-key-!!}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 1. 无需拦截则直接放行
        if (isExclude(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Bearer token
        String token = resolveToken(request);
        if (token == null) {
            return unauthorized(exchange);
        }

        // 3. 解析并校验 token（签名 + 过期）
        Claims claims;
        try {
            claims = parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }

        // 4. 校验 app_key 与路由匹配
        String appKey = claims.get("app_key", String.class);
        String expectedAppKey = matchAppKey(path);
        if (appKey == null || expectedAppKey == null || !appKey.equals(expectedAppKey)) {
            return unauthorized(exchange);
        }

        // 5. 透传用户信息头到下游
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder
                        .header("X-Account-Id", stringValue(claims.get("sub_account_id")))
                        .header("X-Master-Id", stringValue(claims.get("master_account_id")))
                        .header("X-App-Key", appKey)
                        .header("X-Roles", rolesValue(claims.get("roles")))
                        .header("X-Device-Id", stringValue(claims.get("device_id"))))
                .build();

        // 6. 放行
        return chain.filter(mutatedExchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get(AUTH_HEADER);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String value = headers.get(0);
        if (value != null && value.startsWith(BEARER_PREFIX)) {
            return value.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String matchAppKey(String path) {
        Map<String, String> mapping = authProperties.getAppRouteMapping();
        if (mapping == null || mapping.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (antPathMatcher.match(entry.getValue(), path)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String rolesValue(Object value) {
        if (!(value instanceof List<?> roles)) {
            return "";
        }
        return roles.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private boolean isExclude(String antPath) {
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
