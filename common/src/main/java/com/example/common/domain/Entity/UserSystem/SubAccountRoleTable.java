package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_sub_account_role")
public class SubAccountRoleTable {

    /** 马甲账号 id */
    private Long subAccountId;

    /** 角色 id */
    private Long roleId;
}
