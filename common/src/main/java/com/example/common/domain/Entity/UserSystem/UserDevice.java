package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-设备关联（user_device，多对多，含启用/禁用）。
 */
@Data
@TableName("user_device")
public class UserDevice {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID（sys_user.id） */
    private Long userId;

    /** 设备 ID（device.id） */
    private Long deviceId;

    /** 状态：1 启用 / 0 禁用（用户可禁用某台设备） */
    private Integer status;

    /** 最近登录时间（便于管理端展示） */
    private LocalDateTime lastLoginTime;

    /** 绑定时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
