package com.example.user.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 验证码缓存。
 *
 * <p>key：{@code code:{type}:{email}}；value：验证码字符串；有效期 50 秒。</p>
 */
@Component
@RequiredArgsConstructor
public class CodeCache {

    private static final String KEY_PREFIX = "code:";
    private static final long TTL_SECONDS = 50;

    private final StringRedisTemplate redis;

    private String key(String email, Integer type) {
        return KEY_PREFIX + type + ":" + email;
    }

    /**
     * 判断验证码是否已存在（存在即处于冷却中）。
     */
    public boolean exists(String email, Integer type) {
        return Boolean.TRUE.equals(redis.hasKey(key(email, type)));
    }

    /**
     * 写入验证码，有效期 50 秒。
     */
    public void put(String email, Integer type, String code) {
        redis.opsForValue().set(key(email, type), code, TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 读取验证码（不存在时返回 null）。
     */
    public String get(String email, Integer type) {
        return redis.opsForValue().get(key(email, type));
    }

    /**
     * 删除验证码。
     */
    public void delete(String email, Integer type) {
        redis.delete(key(email, type));
    }
}
