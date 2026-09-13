package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sub_account")
public class SubAccountTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 邮箱唯一键 */
    private String email;

    /** 所属主账号 */
    private Long masterAccountId;

    /** 马甲名 / 备注 */
    private String aliasName;

    /** 0 正常 1 用户停用 2 管理员禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
