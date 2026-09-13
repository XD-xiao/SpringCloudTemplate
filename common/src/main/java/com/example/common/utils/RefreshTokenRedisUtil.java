package com.example.common.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 元数据的 Redis 存取工具。
 * key 由调用方拼接为 login:RefreshToken:{masterId}:{subId}:{deviceId}:{appKey}:{nonce}。
 */
public final class RefreshTokenRedisUtil {

    private RefreshTokenRedisUtil() {
    }

    /**
     * 保存元数据（仅 roles）
     */
    public static void save(StringRedisTemplate redis, String key, RefreshTokenMeta meta, long ttlDays) {
        redis.opsForValue().set(key, toJson(meta), ttlDays, TimeUnit.DAYS);
    }

    /**
     * 读取元数据，key 不存在或数据不完整时返回 null
     */
    public static RefreshTokenMeta get(StringRedisTemplate redis, String key) {
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        return fromJson(json);
    }

    /**
     * 删除元数据
     */
    public static void delete(StringRedisTemplate redis, String key) {
        redis.delete(key);
    }

    /**
     * 按 pattern 删除所有匹配 key，用于主账号 / 马甲账号 / 设备下线
     */
    public static long deleteByPattern(StringRedisTemplate redis, String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (keys.isEmpty()) {
            return 0;
        }
        Long deleted = redis.delete(keys);
        return deleted == null ? 0 : deleted;
    }

    private static String toJson(RefreshTokenMeta meta) {
        JSONObject obj = new JSONObject();
        obj.set("roles", meta.roles());
        return obj.toString();
    }

    private static RefreshTokenMeta fromJson(String json) {
        JSONObject obj = JSONUtil.parseObj(json);
        JSONArray rolesArr = obj.getJSONArray("roles");
        if (rolesArr == null) {
            return null;
        }
        return new RefreshTokenMeta(rolesArr.toList(String.class));
    }
}
