package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLogTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 马甲账号 id */
    private Long accountId;

    /** 设备展示名（默认为空，用户可自定义） */
    private String deviceName;

    /** 浏览器指纹 deviceId（16 位稳定唯一 ID） */
    private String deviceId;

    /** 所登录系统 */
    private String appKey;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** pc / mobile / tablet */
    private String deviceType;

    /** 原始 UA（辅助识别） */
    private String userAgent;

    /** 时区 */
    private String timeZone;

    /** 经纬度 */
    private String location;

    /** 登录 IP（服务器端获取） */
    private String ip;

    /** IP 归属地（可选） */
    private String ipRegion;

    /** 1 成功 0 失败 */
    private Integer result;

    /** 失败原因（result=0 时） */
    private String failReason;

    private LocalDateTime createTime;
}
