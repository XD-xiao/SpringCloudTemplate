package com.example.gateway.filters;

import com.example.common.auth.AuthHeaders;
import com.example.common.auth.JwtUtils;
import com.example.common.auth.UserIdentity;
import com.example.common.auth.UserIdentityCodec;
import com.example.common.domain.Result;
import com.example.gateway.config.AuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /** JWT 签名密钥（HS256，需与签发方一致；无默认值，缺失时启动失败以便尽早暴露配置问题） */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 1. 无条件清除外部传入的身份头（白名单），防止绕过网关伪造身份。
        //    放行路径同样必须清除，否则下游会误信客户端伪造的身份头。
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers ->
                        headers.keySet().removeIf(AuthHeaders::isIdentityHeader)))
                .build();

        // 2. 无需拦截则直接放行（身份头已在第 1 步清除）
        if (isExclude(path)) {
            return chain.filter(sanitizedExchange);
        }

        // 3. 提取 Bearer token
        String token = resolveToken(request);
        if (token == null) {
            return unauthorized(exchange);
        }

        // 4. 解析并校验 token（签名 + 过期），并统一解码为身份模型
        UserIdentity identity;
        try {
            Claims claims = JwtUtils.parseToken(jwtSecret, token);
            identity = UserIdentityCodec.fromClaims(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }

        // 5. 校验 app_key 与路由匹配
        String expectedAppKey = matchAppKey(path);
        if (identity.appKey() == null || !identity.appKey().equals(expectedAppKey)) {
            return unauthorized(exchange);
        }

        // 6. 按统一契约透传身份头到下游（在已清除身份头的基础上写入）
        Map<String, String> identityHeaders = UserIdentityCodec.toHeaders(identity);
        ServerWebExchange mutatedExchange = sanitizedExchange.mutate()
                .request(builder -> builder.headers(headers ->
                        identityHeaders.forEach(headers::set)))
                .build();

        // 7. 放行
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

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 与下游保持一致，统一返回 Result JSON，便于前端拦截器处理
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Result.error(401, "未认证或 token 失效"));
        } catch (JsonProcessingException e) {
            body = "{\"code\":401,\"message\":\"未认证或 token 失效\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
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
