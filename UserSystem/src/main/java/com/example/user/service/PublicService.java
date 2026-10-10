package com.example.user.service;

import java.math.BigDecimal;

public interface PublicService {

    /**
     * 发送验证码。
     *
     * @param email 邮箱
     * @param type  类型（1 注册 2 登录）
     */
    void getCode(String email, Integer type);

    /**
     * 注册。
     */
    void register(String email, String password, String code);

    /**
     * 密码登录。
     *
     * @return 登录结果（短 Token + 权限点 + 长 Token；长 Token 由 Controller 写入 Cookie）
     */
    AuthResult loginByPassword(String email, String password,
                               String fingerprint, String deviceType, String deviceCategory,
                               String osName, String osVersion, String browserName, String browserVersion,
                               String userAgent, String loginIp, String timezone, BigDecimal latitude, BigDecimal longitude);

    /**
     * 验证码登录。
     */
    AuthResult loginByCode(String email, String code,
                           String fingerprint, String deviceType, String deviceCategory,
                           String osName, String osVersion, String browserName, String browserVersion,
                           String userAgent, String loginIp, String timezone, BigDecimal latitude, BigDecimal longitude);

    /**
     * 刷新令牌。
     *
     * @param refreshToken 从 Cookie 读取的长 Token
     * @return 刷新结果（新短 Token + 权限点 + 新长 Token；新长 Token 由 Controller 写入 Cookie）
     */
    AuthResult refresh(String refreshToken);

    /**
     * 登出：撤销当前设备会话（删除对应 Redis key）。
     *
     * @param refreshToken 从 Cookie 读取的长 Token（已过期/无效时忽略）
     */
    void logout(String refreshToken);

}
