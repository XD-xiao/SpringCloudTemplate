package com.example.common.utils;

import java.util.List;

/**
 * Refresh Token 在 Redis 中保存的元数据（仅角色集合）
 */
public record RefreshTokenMeta(List<String> roles) {
}
