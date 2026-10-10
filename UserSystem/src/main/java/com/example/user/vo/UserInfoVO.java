package com.example.user.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象（sys_user 与 user_profile 合并）。
 *
 * <p>用于“获得用户信息”与“获得用户列表”的返回，避免直接暴露密码等敏感字段。</p>
 */
@Data
@NoArgsConstructor
public class UserInfoVO {

    /** 用户 ID（sys_user.id） */
    private Long userId;

    /** 登录邮箱 */
    private String email;

    /** 账号状态：1 启用 / 0 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

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
}
