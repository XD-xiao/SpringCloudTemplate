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
     */
    void loginByPassword(String email, String password,
                         String fingerprint, String deviceType, String deviceCategory,
                         String osName, String osVersion, String browserName, String browserVersion,
                         String userAgent, String timezone, BigDecimal latitude, BigDecimal longitude);

    /**
     * 验证码登录。
     */
    void loginByCode(String email, String code,
                     String fingerprint, String deviceType, String deviceCategory,
                     String osName, String osVersion, String browserName, String browserVersion,
                     String userAgent, String timezone, BigDecimal latitude, BigDecimal longitude);

    /**
     * 刷新令牌。
     */
    void refresh();

}
