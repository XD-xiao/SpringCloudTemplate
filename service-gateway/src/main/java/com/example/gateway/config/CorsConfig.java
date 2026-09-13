package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 网关全局跨域配置。
 *
 * 前端开发服务器（如 http://localhost:5173）与网关（http://localhost:8080）不同源，
 * 需要在网关侧放行 CORS，否则浏览器会拦截跨域请求（net::ERR_FAILED）。
 *
 * 注意：由于登录/刷新等接口依赖 HttpOnly Cookie（RefreshToken-{appKey}），
 * 必须开启 allowCredentials；此时不能使用 allowedOrigins("*")，
 * 改用 allowedOriginPatterns 反射来源 Origin。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许任意来源（开发环境）。生产环境建议收敛为具体域名列表。
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
