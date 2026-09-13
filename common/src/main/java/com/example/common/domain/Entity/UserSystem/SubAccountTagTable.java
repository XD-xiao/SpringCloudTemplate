package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sub_account_tag")
public class SubAccountTagTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String tagName;

    /** 标签标识，如 pref_category */
    private String tagKey;

    /** 1 基础属性 2 行为标签 3 偏好标签 4 AI画像 5 自定义 */
    private Integer tagType;

    /** 1 字符串 2 数值 3 布尔 4 日期 5 JSON */
    private Integer dataType;

    /** 1 注册填写 2 行为分析 3 AI生成 4 运营配置 */
    private Integer source;

    /** 0 单值 1 多值 */
    private Integer isMulti;

    /** 标签说明 */
    private String description;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 排序权重 */
    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
