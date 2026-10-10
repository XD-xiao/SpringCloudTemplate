package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.SysUser;
import com.example.common.domain.Entity.UserSystem.UserProfile;
import com.example.common.domain.PageBean;
import com.example.common.exception.BusinessException;
import com.example.user.cache.RefreshTokenCache;
import com.example.user.mapper.SysUserMapper;
import com.example.user.mapper.UserProfileMapper;
import com.example.user.service.UserService;
import com.example.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link UserService} 实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final RefreshTokenCache refreshTokenCache;

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        return toUserInfoVO(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyUserInfo(Long userId, String nickname, String avatar, Integer gender,
                                 LocalDate birthday, String bio) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(400, "用户信息不存在");
        }
        // 仅更新非空字段，避免把未传入的字段误清空
        if (nickname != null) {
            profile.setNickname(nickname);
        }
        if (avatar != null) {
            profile.setAvatar(avatar);
        }
        if (gender != null) {
            profile.setGender(gender);
        }
        if (birthday != null) {
            profile.setBirthday(birthday);
        }
        if (bio != null) {
            profile.setBio(bio);
        }
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.updateById(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值不合法");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        // 禁用用户时立即踢下线，删除其全部设备会话（Redis）
        if (Integer.valueOf(0).equals(status)) {
            refreshTokenCache.deleteByUser(userId);
        }
    }

    @Override
    public PageBean<UserInfoVO> getUserList(long page, long size, String email, String nickname, Integer gender) {
        // 1. 分页查询 sys_user；昵称/性别过滤用 EXISTS 子查询下沉到数据库，避免全量加载 userId 再拼 IN
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.like(email != null && !email.isBlank(), SysUser::getEmail, email);
        if (nickname != null && !nickname.isBlank()) {
            userWrapper.exists("SELECT 1 FROM user_profile p WHERE p.user_id = sys_user.id AND p.nickname LIKE {0}",
                    "%" + nickname + "%");
        }
        if (gender != null) {
            userWrapper.exists("SELECT 1 FROM user_profile p WHERE p.user_id = sys_user.id AND p.gender = {0}", gender);
        }
        userWrapper.orderByDesc(SysUser::getId);
        Page<SysUser> userPage = sysUserMapper.selectPage(new Page<>(page, size), userWrapper);

        // 2. 批量加载 user_profile 并组装 VO
        List<Long> userIds = userPage.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, UserProfile> profileMap = userIds.isEmpty()
                ? Map.of()
                : userProfileMapper.selectList(
                                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
                        .stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));

        List<UserInfoVO> list = userPage.getRecords().stream()
                .map(u -> toUserInfoVO(u, profileMap.get(u.getId())))
                .toList();
        return new PageBean<>(userPage.getTotal(), list);
    }

    private UserInfoVO toUserInfoVO(SysUser user, UserProfile profile) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        if (profile != null) {
            vo.setNickname(profile.getNickname());
            vo.setAvatar(profile.getAvatar());
            vo.setGender(profile.getGender());
            vo.setBirthday(profile.getBirthday());
            vo.setBio(profile.getBio());
        }
        return vo;
    }
}
