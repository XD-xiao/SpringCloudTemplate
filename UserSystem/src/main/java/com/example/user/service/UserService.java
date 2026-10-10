package com.example.user.service;

import com.example.common.domain.PageBean;
import com.example.user.vo.UserInfoVO;

import java.time.LocalDate;

/**
 * 用户域业务接口（sys_user 与 user_profile）。
 *
 * <p>登录日志与用户设备的查询复用 {@link LoginLogService} / {@link DeviceService}，
 * 本接口仅负责账号与资料本身。</p>
 */
public interface UserService {

    /**
     * 获得用户信息（sys_user 与 user_profile 合并）。
     *
     * @param userId 用户 ID（sys_user.id）
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 修改本人信息（仅更新传入的非空字段）。
     *
     * @param userId   当前登录用户 ID
     * @param nickname 昵称
     * @param avatar   头像 URL
     * @param gender   性别
     * @param birthday 生日
     * @param bio      个性签名
     */
    void updateMyUserInfo(Long userId, String nickname, String avatar, Integer gender,
                          LocalDate birthday, String bio);

    /**
     * 修改用户状态（启用 / 停用）。
     *
     * @param userId 目标用户 ID（sys_user.id）
     * @param status 状态：1 启用 / 0 停用
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 分页查询用户列表（可按邮箱 / 昵称 / 性别过滤）。
     */
    PageBean<UserInfoVO> getUserList(long page, long size, String email, String nickname, Integer gender);
}
