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

    /** 无需认证的路径白名单（Ant 风格）。 */
    private List<String> excludePaths;

    /**
     * 系统标识（app_key）→ 路径前缀（Ant 风格）映射，用于校验 token 的 app_key 与路由匹配。
     */
    private Map<String, String> appRouteMapping;
}
