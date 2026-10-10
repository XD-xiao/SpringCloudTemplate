package com.example.user.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户设备视图对象（user_device 与 device 合并）。
 *
 * <p>用于“获得本人/任意用户设备列表”，在关联表状态字段基础上补充设备详情。</p>
 */
@Data
@NoArgsConstructor
public class UserDeviceVO {

    /** 用户-设备关联 ID（user_device.id） */
    private Long id;

    /** 设备 ID（device.id） */
    private Long deviceId;

    /** 设备指纹 */
    private String fingerprint;

    /** 设备类型：Desktop / Mobile / Tablet */
    private String deviceType;

    /** 设备分类 */
    private String deviceCategory;

    /** 操作系统名称 */
    private String osName;

    /** 操作系统版本 */
    private String osVersion;

    /** 浏览器名称 */
    private String browserName;

    /** 浏览器版本 */
    private String browserVersion;

    /** 原始 User Agent */
    private String userAgent;

    /** 设备状态：1 启用 / 0 禁用 */
    private Integer status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;
}
