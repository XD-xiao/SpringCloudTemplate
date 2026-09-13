package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_application")
public class ApplicationTable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 系统名称 */
    private String appName;

    /** 系统标识：user/mall/filebox */
    private String appKey;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
