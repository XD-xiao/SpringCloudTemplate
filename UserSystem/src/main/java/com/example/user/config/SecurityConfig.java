package com.example.user.config;

import com.example.common.domain.Result;
import com.example.user.security.UserInfoAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 安全配置类。
 *
 * 主要职责：
 * 1. 配置过滤器链（SecurityFilterChain），定义接口的访问控制规则；
 * 2. 将项目自定义的 {@link UserInfoAuthFilter} 注册到认证过滤器之前，用于解析并认证用户信息；
 * 3. 提供 BCrypt 密码编码器，供用户密码加密与校验使用。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 自定义的用户信息认证过滤器。
     * 通过构造器注入（配合 @RequiredArgsConstructor），由 Spring 容器自动装配。
     */
    private final UserInfoAuthFilter userInfoAuthFilter;

    /**
     * 用于将统一返回结构 {@link Result} 序列化为 JSON。
     */
    private final ObjectMapper objectMapper;

    /**
     * 配置安全过滤链，定义请求的认证与授权策略。
     *
     * @param http HttpSecurity 构建器
     * @return 组装完成的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF 防护：项目采用无状态的 token 认证，无需 CSRF。
            .csrf(AbstractHttpConfigurer::disable)
            // 使用无状态会话（不创建、不使用 HttpSession）。
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置接口访问规则。
            .authorizeHttpRequests(auth -> auth
                // 用户服务相关的接口放行，无需认证即可访问。
                .requestMatchers("/api/userservice/public/**").permitAll()
                // 其余请求均需认证后才能访问。
                .anyRequest().authenticated())
            // 统一处理认证失败与越权异常，返回与全局异常处理器一致的 JSON 结构。
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未认证或 token 失效"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问")))
            // 在 Spring Security 默认的用户名密码认证过滤器之前加入自定义过滤器。
            .addFilterBefore(userInfoAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 向响应写入统一的错误 JSON。
     */
    private void writeError(HttpServletResponse response, int httpStatus, int code, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }

    /**
     * 密码编码器。
     * 使用 BCrypt 对密码进行单向哈希，保证存储安全。
     *
     * @return BCrypt 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


