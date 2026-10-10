package com.example.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.auth.JwtUtils;
import com.example.common.auth.RefreshTokenInfo;
import com.example.common.auth.UserIdentity;
import com.example.common.domain.Entity.UserSystem.Device;
import com.example.common.domain.Entity.UserSystem.SysUser;
import com.example.common.domain.Entity.UserSystem.UserDevice;
import com.example.common.domain.Entity.UserSystem.UserProfile;
import com.example.common.exception.BusinessException;
import com.example.common.exception.UnauthorizedException;
import com.example.user.cache.CodeCache;
import com.example.user.cache.RefreshTokenCache;
import com.example.user.config.JwtProperties;
import com.example.user.mapper.SysUserMapper;
import com.example.user.mapper.UserProfileMapper;
import com.example.user.service.AuthResult;
import com.example.user.service.DeviceService;
import com.example.user.service.LoginLogService;
import com.example.user.service.PermissionService;
import com.example.user.service.PublicService;
import com.example.user.service.RoleService;
import com.example.user.utils.SendCode;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@link PublicService} 实现。
 */
@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    /** 应用标识，固定为 UserSystem。 */
    private static final String APP_KEY = "UserSystem";

    /** 长 Token 签发方标记。 */
    private static final String REFRESH_ISSUER = "userservice";

    /** 简单邮箱格式校验。 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CodeCache codeCache;
    private final SendCode sendCode;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final DeviceService deviceService;
    private final LoginLogService loginLogService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RefreshTokenCache refreshTokenCache;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void getCode(String email, Integer type) {
        validateEmail(email);
        if (type == null || (type != 1 && type != 2)) {
            throw new BusinessException(400, "验证码类型错误");
        }
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
        validateEmail(email);
        if (password == null || password.length() < 6) {
            throw new BusinessException(400, "密码长度至少 6 位");
        }
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
    public AuthResult loginByPassword(String email, String password,
                                      String fingerprint, String deviceType, String deviceCategory,
                                      String osName, String osVersion, String browserName, String browserVersion,
                                      String userAgent, String loginIp, String timezone, BigDecimal latitude, BigDecimal longitude) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new BusinessException(400, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "账号或密码错误");
        }
        return issueTokens(user, fingerprint, deviceType, deviceCategory, osName, osVersion, browserName, browserVersion,
                userAgent, loginIp, timezone, latitude, longitude, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResult loginByCode(String email, String code,
                                  String fingerprint, String deviceType, String deviceCategory,
                                  String osName, String osVersion, String browserName, String browserVersion,
                                  String userAgent, String loginIp, String timezone, BigDecimal latitude, BigDecimal longitude) {
        // 先校验账号是否存在与状态，避免验证码被无效账号白白消耗
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new BusinessException(400, "该邮箱未注册");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 再校验登录验证码（类型 2），验证通过后删除
        String cachedCode = codeCache.get(email, 2);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        codeCache.delete(email, 2);

        return issueTokens(user, fingerprint, deviceType, deviceCategory, osName, osVersion, browserName, browserVersion,
                userAgent, loginIp, timezone, latitude, longitude, 2);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        // 1. 本地校验长 Token（签名 + 过期），解析 userId / deviceId / jti
        RefreshTokenInfo info;
        try {
            info = JwtUtils.parseRefreshToken(jwtProperties.getRefreshSecret(), refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("refresh token 无效或已过期");
        }
        if (info.userId() == null) {
            throw new UnauthorizedException("refresh token 无效或已过期");
        }
        Long userId = info.userId();
        String deviceId = info.deviceId();

        // 2. 重取角色与权限（走 RBAC 缓存）
        List<String> roleKeys = roleService.getRoleKeysByUserId(userId);
        List<String> permissions = loadPermissions(userId);

        // 3. Lua 原子轮换：GET + activeJti 比对 + SET
        String newJti = UUID.randomUUID().toString();
        long refreshTtlSeconds = jwtProperties.getRefreshTtl().toSeconds();
        long result = refreshTokenCache.rotate(userId, deviceId, info.jti(), newJti, roleKeys, refreshTtlSeconds);
        if (result == -1) {
            throw new UnauthorizedException("会话已过期，请重新登录");
        }
        if (result == -2) {
            System.err.println("安全事件：refresh token 重放检测命中 -> userId=" + userId + ", deviceId=" + deviceId);
            throw new UnauthorizedException("token 已被使用，请重新登录");
        }

        // 4. 重新签发双 Token
        UserIdentity identity = new UserIdentity(userId, deviceId, APP_KEY, roleKeys);
        String accessToken = JwtUtils.createToken(jwtProperties.getSecret(), identity, jwtProperties.getAccessTtl().toMillis());
        String newRefreshToken = JwtUtils.createRefreshToken(jwtProperties.getRefreshSecret(), userId, deviceId, newJti,
                jwtProperties.getRefreshTtl().toMillis(), REFRESH_ISSUER);
        return new AuthResult(accessToken, jwtProperties.getAccessTtl().toSeconds(), permissions, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            RefreshTokenInfo info = JwtUtils.parseRefreshToken(jwtProperties.getRefreshSecret(), refreshToken);
            if (info.userId() != null) {
                refreshTokenCache.deleteByDevice(info.userId(), info.deviceId());
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // token 已过期或无效：会话大概率已随 TTL 过期，忽略即可
        }
    }

    /**
     * 登录成功后签发双 Token、写入会话并记录登录日志。
     */
    private AuthResult issueTokens(SysUser user,
                                   String fingerprint, String deviceType, String deviceCategory,
                                   String osName, String osVersion, String browserName, String browserVersion,
                                   String userAgent, String loginIp, String timezone, BigDecimal latitude, BigDecimal longitude,
                                   int loginType) {
        // 记录设备并校验设备状态
        Device device = deviceService.recordDevice(fingerprint, deviceType, deviceCategory, osName, osVersion, browserName, browserVersion, userAgent);
        UserDevice userDevice = deviceService.getUserDevice(user.getId(), device.getId());
        if (userDevice != null && userDevice.getStatus() != null && userDevice.getStatus() != 1) {
            throw new BusinessException(403, "该设备已被禁用");
        }
        deviceService.bindUserDevice(user.getId(), device.getId());

        // 读取角色标识与权限点（类型 1/2）
        List<String> roleKeys = roleService.getRoleKeysByUserId(user.getId());
        List<String> permissions = loadPermissions(user.getId());

        // 写入会话（Redis）
        String jti = UUID.randomUUID().toString();
        long refreshTtlSeconds = jwtProperties.getRefreshTtl().toSeconds();
        refreshTokenCache.saveSession(user.getId(), fingerprint, roleKeys, jti, refreshTtlSeconds);

        // 签发双 Token
        UserIdentity identity = new UserIdentity(user.getId(), fingerprint, APP_KEY, roleKeys);
        String accessToken = JwtUtils.createToken(jwtProperties.getSecret(), identity, jwtProperties.getAccessTtl().toMillis());
        String refreshToken = JwtUtils.createRefreshToken(jwtProperties.getRefreshSecret(), user.getId(), fingerprint, jti,
                jwtProperties.getRefreshTtl().toMillis(), REFRESH_ISSUER);

        // 记录登录日志
        loginLogService.recordLoginLog(user.getId(), device.getId(), loginIp, timezone, latitude, longitude, loginType, 1);

        return new AuthResult(accessToken, jwtProperties.getAccessTtl().toSeconds(), permissions, refreshToken);
    }

    /**
     * 读取用户类型为 1（页面）/ 2（组件）的权限点标识集合。
     */
    private List<String> loadPermissions(Long userId) {
        Set<String> permKeys = new LinkedHashSet<>();
        for (Long roleId : roleService.getRoleIdsByUserId(userId)) {
            permKeys.addAll(permissionService.getPermKeysByRoleId(roleId, 1, 2));
        }
        return new ArrayList<>(permKeys);
    }

    /**
     * 类型转换：1 注册 2 登录。
     */
    private String typeName(Integer type) {
        return Integer.valueOf(2).equals(type) ? "login" : "register";
    }

    /**
     * 简单邮箱格式校验。
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
    }
}
