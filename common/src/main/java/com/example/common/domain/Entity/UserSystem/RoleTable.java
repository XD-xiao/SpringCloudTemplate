package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class RoleTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属系统 */
    private Long appId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识，如 admin */
    private String roleKey;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
