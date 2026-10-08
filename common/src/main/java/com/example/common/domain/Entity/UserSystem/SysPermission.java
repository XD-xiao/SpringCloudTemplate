package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RBAC 权限点（sys_permission，扁平结构，无父子关系）。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 权限名称 */
    private String permName;

    /** 权限标识（如 user:app:list） */
    private String permKey;

    /** 权限类型：1 页面 / 2 组件 / 3 接口 */
    private Integer permType;

    /** 页面路由或接口路径，组件可空 */
    private String path;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
