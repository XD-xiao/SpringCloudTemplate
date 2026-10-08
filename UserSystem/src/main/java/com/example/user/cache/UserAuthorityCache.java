package com.example.user.cache;

import cn.hutool.json.JSONUtil;
import com.example.common.utils.RefreshTokenRedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户权限标识（perm_key）缓存。
 *
 * <p>鉴权过滤器每次请求都要解析用户的权限集合，属于高频读路径。
 * 这里采用缓存旁路（Cache-Aside）模式：读取时优先命中缓存，未命中再查库并回填；
 * 角色/权限/授权关系变更时通过 {@link #evict} 或 {@link #evictAll} 主动失效，保证一致性。</p>
 *
 * <p>key：{@code auth:authorities:{userId}}；value：JSON 数组，如 {@code ["user:app:list"]}。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAuthorityCache {

    private static final String KEY_PREFIX = "auth:authorities:";
    private static final long TTL_MINUTES = 30;

    private final StringRedisTemplate redis;

    /**
     * 读取用户权限集合；缓存不存在返回 {@code null}，以区分「已缓存的空集合」。
     */
    public List<String> get(Long userId) {
        if (userId == null) {
            return null;
        }
        String json = redis.opsForValue().get(key(userId));
        if (json == null) {
            return null;
        }
        return JSONUtil.parseArray(json).toList(String.class);
    }

    /**
     * 写入用户权限集合。
     */
    public void put(Long userId, List<String> authorities) {
        if (userId == null) {
            return;
        }
        List<String> values = authorities == null ? List.of() : authorities;
        redis.opsForValue().set(key(userId), JSONUtil.toJsonStr(values), TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 失效指定用户的权限缓存。
     */
    public void evict(Long userId) {
        if (userId != null) {
            redis.delete(key(userId));
        }
    }

    /**
     * 失效全部用户的权限缓存（角色或权限本身变更时使用）。
     */
    public void evictAll() {
        RefreshTokenRedisUtil.deleteByPattern(redis, KEY_PREFIX + "*");
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
