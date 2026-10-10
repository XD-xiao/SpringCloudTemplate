package com.example.user.service;

import com.example.common.domain.Entity.UserSystem.LoginLog;
import com.example.common.domain.PageBean;

import java.math.BigDecimal;

/**
 * 登录记录域业务接口。
 *
 * <p>覆盖登录记录（login_log）的记录与查询，供登录流程与管理端使用。</p>
 */
public interface LoginLogService {

    /**
     * 记录一条登录日志（成功或失败均记录）。
     *
     * @param userId      用户 ID
     * @param deviceId    设备 ID（设备无法识别时可为 {@code null}）
     * @param loginIp     登录 IP
     * @param timezone    时区（IANA，如 Asia/Shanghai）
     * @param latitude    纬度（未授权时为 {@code null}）
     * @param longitude   经度（未授权时为 {@code null}）
     * @param loginType   登录方式：1 密码 / 2 验证码 / 3 第三方
     * @param loginResult 登录结果：1 成功 / 0 失败
     */
    void recordLoginLog(Long userId, Long deviceId, String loginIp,
                        String timezone, BigDecimal latitude, BigDecimal longitude,
                        Integer loginType, Integer loginResult);

    /**
     * 按 ID 查询登录日志。
     */
    LoginLog getLoginLogById(Long id);

    /**
     * 分页查询登录日志（可按用户过滤）。
     */
    PageBean<LoginLog> getLoginLogPage(long page, long size, Long userId);
}
