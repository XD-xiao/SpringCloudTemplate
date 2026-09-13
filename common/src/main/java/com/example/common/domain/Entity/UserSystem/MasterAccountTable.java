package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_master_account")
public class MasterAccountTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 身份证号，唯一账号 */
    private String idCard;

    /** 助记词 BCrypt 密文 */
    private String mnemonic;

    /** 1 正常 2 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
