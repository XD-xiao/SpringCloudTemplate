package com.example.common.auth;

/**
 * 长 Token（Refresh Token）解析结果。
 *
 * @param userId   用户 ID（对应会话 key 的 userId）
 * @param deviceId 设备指纹（对应会话 key 的 deviceId，可为空）
 * @param jti      本次长 Token 的 ID（用于重放检测）
 */
public record RefreshTokenInfo(Long userId, String deviceId, String jti) {
}
