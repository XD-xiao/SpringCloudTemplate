package com.example.common.auth;

import java.util.List;

/**
 * 当前登录用户的统一身份模型。
 *
 * <p>标准字段覆盖框架内置的身份信息，字段的 claim 名与头名统一由
 * {@link AuthHeaders} 定义。</p>
 *
 * @param userId    用户 id
 * @param deviceId 设备 id
 * @param appKey   应用标识
 * @param roles    角色集合
 */
public record UserIdentity(
        Long userId,
        String deviceId,
        String appKey,
        List<String> roles) {

    /** 空身份，表示未携带任何身份信息。 */
    public static final UserIdentity EMPTY =
            new UserIdentity(null, null, null, List.of());

    public UserIdentity {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /** 是否已认证（存在账号标识）。 */
    public boolean isPresent() {
        return userId != null;
    }
}
