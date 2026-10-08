package com.example.common.domain.Entity.UserSystem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息（user_profile，与 sys_user 一对一）。
 */
@Data
@TableName("user_profile")
public class UserProfile {

    /** 主键（雪花 ID） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 sys_user.id，一对一 */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 性别：0 未知 / 1 男 / 2 女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 个性签名 / 简介 */
    private String bio;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
