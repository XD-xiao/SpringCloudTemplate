package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_device")
public class LoginDeviceTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 马甲账号 id */
    private Long accountId;

    /** 设备展示名（默认为空，用户可自定义） */
    private String deviceName;

    /** 浏览器指纹 deviceId（16 位稳定唯一 ID） */
    private String deviceId;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最近活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 1 在线 2 已下线 3 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
