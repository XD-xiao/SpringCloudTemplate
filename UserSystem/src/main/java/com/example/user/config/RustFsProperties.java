package com.example.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RustFS（S3 兼容）相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rustfs")
public class RustFsProperties {

    /** S3 兼容 API 地址（后端上传使用）。 */
    private String endpoint;

    /** 访问密钥。 */
    private String accessKey;

    /** 私有密钥。 */
    private String secretKey;

    /** 区域标识（S3 兼容端点通常忽略，但 SDK 要求非空）。 */
    private String region = "us-east-1";

    /** 公共读存储桶名。 */
    private String bucket;

    /** 公共读访问基址（含桶名，供前端展示）。 */
    private String publicBaseUrl;
}
