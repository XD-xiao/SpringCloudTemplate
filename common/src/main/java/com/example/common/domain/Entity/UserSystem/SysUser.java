package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RBAC 用户（sys_user）。
 *
 * <p>仅保留鉴权必需字段，业务信息由各业务模块另建信息表通过 user_id 关联。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号（本模块为邮箱） */
    private String email;

    /** BCrypt 密码密文 */
    private String password;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
