package com.example.gateway.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "sw.auth")
public class AuthProperties {
    private List<String> includePaths;
    private List<String> excludePaths;
    /**
     * 系统-路径前缀映射，用于校验 token 的 app_key 与路由匹配。
     * key 为系统标识（app_key），value 为路径前缀（Ant 风格）。
     */
    private Map<String, String> appRouteMapping;
}
