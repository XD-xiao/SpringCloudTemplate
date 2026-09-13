package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_mnemonic_word")
public class MnemonicWordTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 2 字词组 */
    private String word;

    /** 1 启用 0 停用 */
    private Integer status;
}
