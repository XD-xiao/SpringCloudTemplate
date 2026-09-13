package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class PermissionTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属系统 */
    private Long appId;

    /** 权限名称 */
    private String permName;

    /** 权限标识，如 user:master:list */
    private String permKey;

    /** 1 页面 2 组件 3 接口 */
    private Integer permType;

    /** 父权限 id，0 表示根 */
    private Long parentId;

    /** 前端路由 / 接口路径 */
    private String path;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
