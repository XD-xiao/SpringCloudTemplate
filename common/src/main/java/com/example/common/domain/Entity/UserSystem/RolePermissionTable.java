package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class RolePermissionTable {

    /** 角色 id */
    private Long roleId;

    /** 权限 id */
    private Long permissionId;
}
