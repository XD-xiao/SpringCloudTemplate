package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备（device，设备指纹 + 软硬件信息，独立于用户）。
 */
@Data
@TableName("device")
public class Device {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备指纹，对应前端 deviceId，业务唯一键 */
    private String fingerprint;

    /** 设备类型：Desktop / Mobile / Tablet */
    private String deviceType;

    /** 设备分类：桌面端 / 移动设备 / 平板设备 */
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

    /** 首次出现时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
