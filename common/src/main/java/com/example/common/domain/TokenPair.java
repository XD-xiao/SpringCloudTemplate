package com.example.common.domain;

import java.util.List;

/**
 * 登录 / 刷新返回的短 Token（Access Token）结果。
 *
 * <p>长 Token（Refresh Token）不进入响应体，仅通过 HttpOnly Cookie 下发。</p>
 *
 * @param accessToken 短 Token（原始 JWT）
 * @param expiresIn   短 Token 有效期（秒）
 * @param permissions 用户可用的权限点标识（perm_key）集合（类型 1 页面 / 2 组件）
 */
public record TokenPair(String accessToken, long expiresIn, List<String> permissions) {

    public TokenPair {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
