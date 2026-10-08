package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登录记录（login_log）。
 */
@Data
@TableName("login_log")
public class LoginLog {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID（sys_user.id） */
    private Long userId;

    /** 设备 ID（device.id），设备无法识别时为空 */
    private Long deviceId;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 登录 IP */
    private String loginIp;

    /** 时区（IANA，如 Asia/Shanghai） */
    private String timezone;

    /** 纬度（未授权时为 null） */
    private BigDecimal latitude;

    /** 经度（未授权时为 null） */
    private BigDecimal longitude;

    /** 登录方式：1 密码 / 2 验证码 / 3 第三方 */
    private Integer loginType;

    /** 登录结果：1 成功 / 0 失败 */
    private Integer loginResult;
}
