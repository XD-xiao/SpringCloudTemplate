package com.example.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 相关配置。
 *
 * <p>短 Token 签名密钥 {@code jwt.secret} 与网关共享（由 Nacos {@code common.yaml} 提供）；
 * 长 Token 签名密钥 {@code jwt.refresh-secret} 仅 UserSystem 持有，二者隔离。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 短 Token 签名密钥（HS256，与网关一致）。 */
    private String secret;

    /** 长 Token 签名密钥（HS256，仅 UserSystem 持有）。 */
    private String refreshSecret;

    /** 短 Token 有效期。 */
    private Duration accessTtl = Duration.ofMinutes(15);

    /** 长 Token / 会话有效期（滑动）。 */
    private Duration refreshTtl = Duration.ofDays(30);
}
