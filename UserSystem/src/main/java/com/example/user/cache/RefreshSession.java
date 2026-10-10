package com.example.user.cache;

import java.util.List;

/**
 * 登录会话记录（Redis 中 {@code auth:session:{userId}:{deviceId}} 的 value）。
 *
 * @param userId    用户 ID
 * @param deviceId  设备指纹
 * @param roles     角色标识（role_key）集合
 * @param activeJti 当前有效长 Token 的 jti（重放检测比对基准）
 */
public record RefreshSession(Long userId, String deviceId, List<String> roles, String activeJti) {
}
