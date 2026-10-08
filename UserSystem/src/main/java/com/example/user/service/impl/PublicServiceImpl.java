package com.example.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.domain.Entity.UserSystem.SysUser;
import com.example.common.domain.Entity.UserSystem.UserProfile;
import com.example.common.exception.BusinessException;
import com.example.user.cache.CodeCache;
import com.example.user.mapper.SysUserMapper;
import com.example.user.mapper.UserProfileMapper;
import com.example.user.service.PublicService;
import com.example.user.utils.SendCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@link PublicService} 实现。
 */
@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final CodeCache codeCache;
    private final SendCode sendCode;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void getCode(String email, Integer type) {
        // 冷却中：同邮箱同类型 50 秒内已发送过验证码
        if (codeCache.exists(email, type)) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        // 生成 4 位数字验证码
        String code = RandomUtil.randomNumbers(4);

        // 以 邮箱+类型 作为 key 存入 Redis（有效期 50 秒）
        codeCache.put(email, type, code);

        // 发送邮件，并在控制台打印
        sendCode.sendCode(email, code, typeName(type));
        System.out.println("验证码发送 -> email: " + email + ", code: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String password, String code) {
        // 1. 校验邮箱是否已注册
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (count != null && count > 0) {
            throw new BusinessException(400, "该邮箱已注册，请直接登录");
        }

        // 2. 校验注册验证码（类型 1），验证通过后删除 Redis 中的验证码
        String cachedCode = codeCache.get(email, 1);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        codeCache.delete(email, 1);

        // 3. 初始化用户（sys_user），密码加密存储，状态默认为 1 启用
        SysUser user = new SysUser();
        user.setEmail(email);
        password = passwordEncoder.encode(password);
        user.setPassword(password);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        // 4. 初始化用户信息（user_profile）
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(RandomUtil.randomString(8));
        profile.setAvatar("");
        profile.setGender(0);
        profile.setBirthday(LocalDate.of(2000, 10, 1));
        profile.setBio("沉默是今晚的康桥");
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loginByPassword(String email, String password,
                                String fingerprint, String deviceType, String deviceCategory,
                                String osName, String osVersion, String browserName, String browserVersion,
                                String userAgent, String timezone, BigDecimal latitude, BigDecimal longitude) {
        // TODO 具体实现
        // 判断邮箱是否存在、状态是否启用 以及密码是否正确

        // 


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loginByCode(String email, String code,
                            String fingerprint, String deviceType, String deviceCategory,
                            String osName, String osVersion, String browserName, String browserVersion,
                            String userAgent, String timezone, BigDecimal latitude, BigDecimal longitude) {
        // TODO 具体实现
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refresh() {
        // TODO 具体实现
    }

    /**
     * 类型转换：1 注册 2 登录。
     */
    private String typeName(Integer type) {
        return Integer.valueOf(2).equals(type) ? "login" : "register";
    }
}
