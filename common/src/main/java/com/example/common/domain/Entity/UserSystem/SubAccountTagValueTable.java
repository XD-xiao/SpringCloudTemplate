package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sub_account_tag_value")
public class SubAccountTagValueTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 马甲账号 id */
    private Long subAccountId;

    /** 标签 id */
    private Long tagId;

    /** 标签值，按 data_type 解析；JSON 存字符串 */
    private String tagValue;

    /** 该值来源：1 注册填写 2 行为分析 3 AI生成 4 运营配置 */
    private Integer source;

    private LocalDateTime updateTime;
}
