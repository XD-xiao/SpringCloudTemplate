package com.example.api.config;

import com.example.common.auth.AuthHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

public class DefaultFeignConfig {

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前请求上下文获取原始请求
                if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                    return;
                }
                HttpServletRequest request = attributes.getRequest();
                // 透传原始凭证
                copyHeader(request, template, AuthHeaders.AUTHORIZATION);
                // 按约定前缀整批透传身份头：网关新增字段时无需改动此处
                Enumeration<String> names = request.getHeaderNames();
                while (names != null && names.hasMoreElements()) {
                    String name = names.nextElement();
                    if (AuthHeaders.isIdentityHeader(name)) {
                        copyHeader(request, template, name);
                    }
                }
            }
        };
    }

    private void copyHeader(HttpServletRequest request, RequestTemplate template, String header) {
        String value = request.getHeader(header);
        if (value != null && !value.isBlank()) {
            template.header(header, value);
        }
    }
}
