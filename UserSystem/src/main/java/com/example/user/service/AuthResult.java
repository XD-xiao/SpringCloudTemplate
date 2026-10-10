package com.example.user.service;

import java.util.List;

/**
 * 登录 / 刷新内部结果。
 *
 * <p>除响应体字段（短 Token + 权限点）外，额外携带需由 Controller 写入 Cookie 的长 Token。
 * 长 Token 不进入响应体。</p>
 *
 * @param accessToken  短 Token（原始 JWT）
 * @param expiresIn    短 Token 有效期（秒）
 * @param permissions  权限点标识（perm_key）集合
 * @param refreshToken 长 Token（写入 HttpOnly Cookie）
 */
public record AuthResult(String accessToken, long expiresIn, List<String> permissions, String refreshToken) {

    public AuthResult {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
