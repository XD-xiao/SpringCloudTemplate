package com.example.api.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public class DefaultFeignConfig {

    /**
     * 需要从当前请求透传到下游微服务的用户信息头。
     */
    private static final List<String> USER_INFO_HEADERS = List.of(
            "Authorization",
            "X-Account-Id",
            "X-Master-Id",
            "X-App-Key",
            "X-Roles",
            "X-Device-Id"
    );

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前请求上下文获取原始请求，透传用户信息头到下游微服务
                if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                    return;
                }
                HttpServletRequest request = attributes.getRequest();
                for (String header : USER_INFO_HEADERS) {
                    String value = request.getHeader(header);
                    if (value != null && !value.isBlank()) {
                        template.header(header, value);
                    }
                }
            }
        };
    }
}
