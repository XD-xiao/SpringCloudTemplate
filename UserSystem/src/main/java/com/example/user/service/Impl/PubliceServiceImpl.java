package com.example.user.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.domain.Entity.UserSystem.ApplicationTable;
import com.example.common.domain.Entity.UserSystem.LoginDeviceTable;
import com.example.common.domain.Entity.UserSystem.LoginLogTable;
import com.example.common.domain.Entity.UserSystem.MasterAccountTable;
import com.example.common.domain.Entity.UserSystem.MnemonicWordTable;
import com.example.common.domain.Entity.UserSystem.PermissionTable;
import com.example.common.domain.Entity.UserSystem.RolePermissionTable;
import com.example.common.domain.Entity.UserSystem.RoleTable;
import com.example.common.domain.Entity.UserSystem.SubAccountRoleTable;
import com.example.common.domain.Entity.UserSystem.SubAccountTable;
import com.example.common.domain.vo.UserSystem.LoginResultVO;
import com.example.common.exception.BusinessException;
import com.example.common.utils.RefreshTokenMeta;
import com.example.common.utils.RefreshTokenRedisUtil;
import com.example.user.mapper.*;
import com.example.user.service.PublicService;
import com.example.user.utils.SendCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PubliceServiceImpl implements PublicService {

    /** 返回给前端的令牌前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    // ============ 从 application.yml 读取的业务配置 ============

    /** 验证码有效期（秒） */
    @Value("${user-system.verify-code.ttl-seconds:50}")
    private long codeTtlSeconds;

    /** 访问令牌（Access Token）有效期（分钟） */
    @Value("${user-system.token.access-ttl-minutes:30}")
    private long accessTtlMinutes;

    /** 刷新令牌（Refresh Token）有效期（天） */
    @Value("${user-system.token.refresh-ttl-days:15}")
    private long refreshTtlDays;

    /** 登录令牌在 Redis 中的 key 前缀 */
    @Value("${user-system.token.key-prefix:login:RefreshToken:}")
    private String keyPrefix;

    /** 应用标识（app_key） */
    @Value("${user-system.app-key:UserSystem}")
    private String appKeyUser;

    /** 刷新令牌存放的 Cookie 名称前缀（实际 Cookie 名称为 {name}-{appKey}） */
    @Value("${user-system.token.refresh-cookie-name:RefreshToken}")
    private String refreshCookieName;

    /** 验证码在 Redis 中的 key 前缀 */
    @Value("${user-system.verify-code.redis-prefix:verify:code:}")
    private String verifyCodePrefix;

    /** 验证码在 Redis 中的 key 前缀 */
    @Value("${user-system.auth-code.redis-prefix:auth:code:}")
    private String authCodePrefix;

    /** 角色缓存 key 前缀 */
    @Value("${user-system.roles-cache.redis-prefix:auth:roles:}")
    private String rolesCachePrefix;

    /** 角色缓存有效期（秒） */
    @Value("${user-system.roles-cache.ttl-seconds:3600}")
    private long rolesCacheTtlSeconds;

    /** 权限点缓存 key 前缀 */
    @Value("${user-system.permissions-cache.redis-prefix:auth:permission:}")
    private String permissionsCachePrefix;

    /** 权限点缓存有效期（秒） */
    @Value("${user-system.permissions-cache.ttl-seconds:3600}")
    private long permissionsCacheTtlSeconds;

    /** 注册场景使用的验证码类型标识 */
    @Value("${user-system.verify-code.register-type:register}")
    private String registerType;

    @Value("${user-system.verify-code.login-type:login}")
    private String verifyloginType;

    /** 生成授权码场景使用的类型标识 */
    @Value("${user-system.auth-code.authorize-type:authorize}")
    private String authorizeType;

    /** JWT 签名密钥 */
    @Value("${jwt.secret:change-me-to-a-32-bytes-secret-key-!!}")
    private String jwtSecret;

    private final StringRedisTemplate stringRedisTemplate;
    private final SendCode sendCode;

    private final ApplicationMapper applicationMapper;
    private final LoginDeviceMapper loginDeviceMapper;
    private final LoginLogMapper loginLogMapper;
    private final MasterAccountMapper masterAccountMapper;
    private final MnemonicWordMapper mnemonicWordMapper;
    private final SubAccountMapper subAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final SubAccountRoleMapper subAccountRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public List<String> registerMaster(String idCard, String email, String code) {

        // 在redis判断是否有相同功能的验证码,appKey=UserSystem,type=login,没有则返回验证码错误
        String redisKey = verifyCodePrefix + appKeyUser + ":" + registerType + ":" + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证通过后删除，防止验证码复用
        stringRedisTemplate.delete(redisKey);

        // 身份证号唯一性校验
        Long masterCount = masterAccountMapper.selectCount(
                new QueryWrapper<MasterAccountTable>().eq("id_card", idCard));
        if (masterCount != null && masterCount > 0) {
            throw new BusinessException("该身份证号已注册");
        }

        // 邮箱唯一性校验（马甲账号邮箱唯一）
        Long subCount = subAccountMapper.selectCount(
                new QueryWrapper<SubAccountTable>().eq("email", email));
        if (subCount != null && subCount > 0) {
            throw new BusinessException("该邮箱已被使用");
        }

        // 在mnemonicWordMapper抽取5个启用词条
        List<MnemonicWordTable> wordList = mnemonicWordMapper.selectList(
                new QueryWrapper<MnemonicWordTable>().eq("status", 1));
        if (wordList == null || wordList.size() < 5) {
            throw new BusinessException("助记词词库不足");
        }
        Collections.shuffle(wordList);
        List<String> mnemonic = wordList.subList(0, 5).stream()
                .map(MnemonicWordTable::getWord)
                .toList();

        // 拼接明文助记词并 BCrypt 加密保存到数据库
        String plainMnemonic = String.join("", mnemonic);
        String encodedMnemonic = passwordEncoder.encode(plainMnemonic);

        // 创建主账号masterAccountMapper
        MasterAccountTable masterAccount = new MasterAccountTable();
        masterAccount.setIdCard(idCard);
        masterAccount.setMnemonic(encodedMnemonic);
        masterAccount.setStatus(1);
        masterAccountMapper.insert(masterAccount);

        // 创建马甲账号subAccountMapper
        SubAccountTable subAccount = new SubAccountTable();
        subAccount.setEmail(email);
        subAccount.setMasterAccountId(masterAccount.getId());
        subAccount.setStatus(0);
        subAccountMapper.insert(subAccount);

        // 返回结果让用户记住
        return mnemonic;
    }

    @Override
    public LoginResultVO loginMaster(
            String idCard,
            List<String> mnemonic,
            String email,
            String deviceId,
            String browser,
            String os,
            String deviceTyp,
            String userAgent,
            String timeZone,
            String location
    ) {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String ip = request == null ? null : getClientIp(request);

        // 先按邮箱定位马甲账号，用于日志 account_id 与后续归属校验
        SubAccountTable subAccount = subAccountMapper.selectOne(
                new QueryWrapper<SubAccountTable>().eq("email", email));

        // 判断idCard存在，且mnemonic正确
        MasterAccountTable master = masterAccountMapper.selectOne(
                new QueryWrapper<MasterAccountTable>().eq("id_card", idCard));
        String plainMnemonic = String.join("", mnemonic);
        if (master == null || !passwordEncoder.matches(plainMnemonic, master.getMnemonic())) {
            throw new BusinessException("身份证号或助记词错误");
        }
        if (master.getStatus() == null || master.getStatus() != 1) {
            recordLoginLog(subAccount, deviceId, appKeyUser , browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "主账号已停用");
            throw new BusinessException("主账号已停用");
        }

        // 判断email存在且正常，且属于主账号
        if (subAccount == null) {
            throw new BusinessException("邮箱对应的马甲账号不存在");
        }
        if (!master.getId().equals(subAccount.getMasterAccountId())) {
            recordLoginLog(subAccount, deviceId, appKeyUser, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "邮箱不属于该主账号");
            throw new BusinessException("邮箱不属于该主账号");
        }
        if (subAccount.getStatus() == null || subAccount.getStatus() != 0) {
            recordLoginLog(subAccount, deviceId, appKeyUser, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "马甲账号不可用");
            throw new BusinessException("马甲账号不可用");
        }

        // 判断设备是否禁用
        LoginDeviceTable device = loginDeviceMapper.selectOne(
                new QueryWrapper<LoginDeviceTable>()
                        .eq("account_id", subAccount.getId())
                        .eq("device_id", deviceId));
        if (device != null && Integer.valueOf(3).equals(device.getStatus())) {
            recordLoginLog(subAccount, deviceId, appKeyUser, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "设备已被禁用");
            throw new BusinessException("设备已被禁用");
        }

        // 获得权限信息
        AuthInfo authInfo = loadAuthInfo(subAccount.getId(), appKeyUser);

        // 生成长token（JWT，含 masterId/subId/deviceId/appKey/nonce），写入 Redis 与 Cookie
        String nonce = generateNonce();
        String refreshToken = createRefreshToken(master.getId(), subAccount.getId(), deviceId, appKeyUser, nonce);
        saveRefreshToken(master.getId(), subAccount.getId(), deviceId, appKeyUser, nonce, authInfo.roles());
        setRefreshTokenCookie(response, refreshToken);

        // 记录设备与日志
        recordLoginDevice(subAccount.getId(), deviceId, refreshToken, device);
        recordLoginLog(subAccount, deviceId, appKeyUser, browser, os, deviceTyp, userAgent, timeZone, location, ip, 1, null);

        // 生成短token 并返回（携带权限点列表）
        String accessToken = createAccessToken(subAccount.getId(), master.getId(), deviceId, appKeyUser, authInfo.roles());
        return new LoginResultVO(BEARER_PREFIX + accessToken, loadPermissions(appKeyUser, authInfo.roles()));
    }

    @Override
    public LoginResultVO loginSubByEmail(String appKey, String email, String code, String deviceId, String browser, String os, String deviceTyp, String userAgent, String timeZone, String location) {

        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String ip = request == null ? null : getClientIp(request);

        // 通过邮箱在redis校验验证码
        String redisKey = verifyCodePrefix + appKey + ":" + verifyloginType + ":" + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证通过后删除，防止验证码复用
        stringRedisTemplate.delete(redisKey);

        // 判断email 状态正常
        SubAccountTable subAccount = subAccountMapper.selectOne(
                new QueryWrapper<SubAccountTable>().eq("email", email));
        if (subAccount == null) {
            throw new BusinessException("邮箱对应的马甲账号不存在");
        }
        if (subAccount.getStatus() == null || subAccount.getStatus() != 0) {
            recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "马甲账号不可用");
            throw new BusinessException("马甲账号不可用");
        }

        // 判断设备是否禁用
        LoginDeviceTable device = loginDeviceMapper.selectOne(
                new QueryWrapper<LoginDeviceTable>()
                        .eq("account_id", subAccount.getId())
                        .eq("device_id", deviceId));
        if (device != null && Integer.valueOf(3).equals(device.getStatus())) {
            recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "设备已被禁用");
            throw new BusinessException("设备已被禁用");
        }

        // 获取权限信息
        AuthInfo authInfo = loadAuthInfo(subAccount.getId(), appKey);

        // 生成长token（JWT，含 masterId/subId/deviceId/appKey/nonce），写入 Redis 与 Cookie
        String nonce = generateNonce();
        String refreshToken = createRefreshToken(subAccount.getMasterAccountId(), subAccount.getId(), deviceId, appKey, nonce);
        saveRefreshToken(subAccount.getMasterAccountId(), subAccount.getId(), deviceId, appKey, nonce, authInfo.roles());
        setRefreshTokenCookie(response, refreshToken);

        // 记录设备与日志
        recordLoginDevice(subAccount.getId(), deviceId, refreshToken, device);
        recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 1, null);

        // 生成短token，并返回（携带权限点列表）
        String accessToken = createAccessToken(subAccount.getId(), subAccount.getMasterAccountId(), deviceId, appKey, authInfo.roles());
        return new LoginResultVO(BEARER_PREFIX + accessToken, loadPermissions(appKey, authInfo.roles()));
    }

    @Override
    public LoginResultVO loginSubByAuth(String appKey, String email, String code, String deviceId, String browser, String os, String deviceTyp, String userAgent, String timeZone, String location) {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String ip = request == null ? null : getClientIp(request);

        // 通过邮箱在redis校验授权码
        String redisKey = authCodePrefix + appKey + ":" + authorizeType + ":" + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException("授权码错误");
        }
        // 验证通过后删除，防止授权码复用
        stringRedisTemplate.delete(redisKey);

        // 判断email 状态正常
        SubAccountTable subAccount = subAccountMapper.selectOne(
                new QueryWrapper<SubAccountTable>().eq("email", email));
        if (subAccount == null) {
            throw new BusinessException("邮箱对应的马甲账号不存在");
        }
        if (subAccount.getStatus() == null || subAccount.getStatus() != 0) {
            recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "马甲账号不可用");
            throw new BusinessException("马甲账号不可用");
        }

        // 判断设备是否禁用
        LoginDeviceTable device = loginDeviceMapper.selectOne(
                new QueryWrapper<LoginDeviceTable>()
                        .eq("account_id", subAccount.getId())
                        .eq("device_id", deviceId));
        if (device != null && Integer.valueOf(3).equals(device.getStatus())) {
            recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 0, "设备已被禁用");
            throw new BusinessException("设备已被禁用");
        }

        // 获取权限信息
        AuthInfo authInfo = loadAuthInfo(subAccount.getId(), appKey);

        // 生成长token（JWT，含 masterId/subId/deviceId/appKey/nonce），写入 Redis 与 Cookie
        String nonce = generateNonce();
        String refreshToken = createRefreshToken(subAccount.getMasterAccountId(), subAccount.getId(), deviceId, appKey, nonce);
        saveRefreshToken(subAccount.getMasterAccountId(), subAccount.getId(), deviceId, appKey, nonce, authInfo.roles());
        setRefreshTokenCookie(response, refreshToken);

        // 记录设备与日志
        recordLoginDevice(subAccount.getId(), deviceId, refreshToken, device);
        recordLoginLog(subAccount, deviceId, appKey, browser, os, deviceTyp, userAgent, timeZone, location, ip, 1, null);

        // 生成短token，并返回（携带权限点列表）
        String accessToken = createAccessToken(subAccount.getId(), subAccount.getMasterAccountId(), deviceId, appKey, authInfo.roles());
        return new LoginResultVO(BEARER_PREFIX + accessToken, loadPermissions(appKey, authInfo.roles()));

    }

    @Override
    public String sendCode(String email, String appKey, String type) {
        String redisKey = verifyCodePrefix + appKey + ":" + type + ":" + email;

        // 在redis判断是否有相同功能的验证码，有则返回 冷却中
        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        if (hasKey) {
            return "冷却中";
        }

        // 生成随机6位数字验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 写入redis,TTL为50s
        stringRedisTemplate.opsForValue().set(redisKey, code, codeTtlSeconds, TimeUnit.SECONDS);

        // 发送邮件
        sendCode.sendCode(email, code, type);

        System.out.println( email + " : " + code);

        return "发送成功";
    }

    @Override
    public LoginResultVO refreshToken(String deviceId) {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();

        // 在 Cookie 中获取 Refresh Token
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("缺少登录凭证");
        }

        // 解析 JWT，得到 masterId/subId/deviceId/appKey/nonce
        RefreshTokenClaims claims = parseRefreshToken(refreshToken);
        if (claims.masterId() == null || claims.subId() == null || claims.deviceId() == null
                || claims.appKey() == null || claims.nonce() == null) {
            throw new BusinessException("登录凭证无效");
        }

        // 校验请求 deviceId 与 JWT device_id 一致
        if (!deviceId.equals(claims.deviceId())) {
            throw new BusinessException("设备不匹配");
        }

        // 先读旧 key，仅用于校验刷新凭证是否有效
        String oldKey = buildRefreshTokenKey(claims.masterId(), claims.subId(), claims.deviceId(), claims.appKey(), claims.nonce());
        RefreshTokenMeta meta = RefreshTokenRedisUtil.get(stringRedisTemplate, oldKey);
        if (meta == null) {
            throw new BusinessException("登录凭证已失效");
        }

        // 再删除旧 key
        RefreshTokenRedisUtil.delete(stringRedisTemplate, oldKey);

        // 从数据库读取最新角色信息（与登录逻辑保持一致）
        List<String> roles = loadAuthInfo(claims.subId(), claims.appKey()).roles();

        // 写入新 key（新 nonce），并生成新 JWT 返回
        String newNonce = generateNonce();
        String newKey = buildRefreshTokenKey(claims.masterId(), claims.subId(), claims.deviceId(), claims.appKey(), newNonce);
        RefreshTokenRedisUtil.save(stringRedisTemplate, newKey, new RefreshTokenMeta(roles), refreshTtlDays);

        String newRefreshToken = createRefreshToken(claims.masterId(), claims.subId(), claims.deviceId(), claims.appKey(), newNonce);
        setRefreshTokenCookie(response, newRefreshToken);

        // 生成新的 Access Token 返回（携带权限点列表）
        String accessToken = createAccessToken(claims.subId(), claims.masterId(), claims.deviceId(), claims.appKey(), roles);
        return new LoginResultVO(BEARER_PREFIX + accessToken, loadPermissions(claims.appKey(), roles));
    }

    @Override
    @Transactional
    public void importPermissions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }

        ApplicationTable app = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKeyUser));
        if (app == null) {
            throw new BusinessException("未找到应用配置，无法导入权限");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("读取文件失败");
        }

        JSONArray array;
        try {
            array = JSONUtil.parseArray(content);
        } catch (Exception e) {
            throw new BusinessException("权限文件格式错误");
        }

        Set<String> seenKeys = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            String permKey = item.getStr("permKey");
            String permName = item.getStr("permName");
            Integer permType = item.getInt("permType");

            if (permKey == null || permKey.isBlank()) {
                continue;
            }
            if (permType == null || (permType != 1 && permType != 2)) {
                continue;
            }
            if (!seenKeys.add(permKey)) {
                continue;
            }

            String finalName = (permName == null || permName.isBlank()) ? permKey : permName;

            PermissionTable existing = permissionMapper.selectOne(
                    new QueryWrapper<PermissionTable>()
                            .eq("app_id", app.getId())
                            .eq("perm_key", permKey));

            if (existing == null) {
                PermissionTable permission = new PermissionTable();
                permission.setAppId(app.getId());
                permission.setPermName(finalName);
                permission.setPermKey(permKey);
                permission.setPermType(permType);
                permission.setParentId(0L);
                permission.setPath(null);
                permission.setCreateTime(LocalDateTime.now());
                permission.setUpdateTime(LocalDateTime.now());
                permissionMapper.insert(permission);
            } else {
                boolean changed = false;
                if (!finalName.equals(existing.getPermName())) {
                    existing.setPermName(finalName);
                    changed = true;
                }
                if (!Objects.equals(existing.getPermType(), permType)) {
                    existing.setPermType(permType);
                    changed = true;
                }
                if (changed) {
                    existing.setUpdateTime(LocalDateTime.now());
                    permissionMapper.updateById(existing);
                }
            }
        }
    }


    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private HttpServletResponse currentResponse() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getResponse();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private AuthInfo loadAuthInfo(Long subAccountId, String appKey) {
        // 先查 Redis 缓存
        String rolesCacheKey = rolesCachePrefix + appKey + ":" + subAccountId;
        String cachedRoles = stringRedisTemplate.opsForValue().get(rolesCacheKey);
        if (cachedRoles != null) {
            return new AuthInfo(JSONUtil.parseArray(cachedRoles).toList(String.class));
        }

        ApplicationTable userApp = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey).eq("status", 1));
        if (userApp == null) {
            return new AuthInfo(Collections.emptyList());
        }

        List<Long> roleIds = subAccountRoleMapper.selectList(
                        new QueryWrapper<SubAccountRoleTable>().eq("sub_account_id", subAccountId))
                .stream().map(SubAccountRoleTable::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return new AuthInfo(Collections.emptyList());
        }

        List<String> roles = roleMapper.selectList(
                        new QueryWrapper<RoleTable>().in("id", roleIds).eq("app_id", userApp.getId()))
                .stream().map(RoleTable::getRoleKey).toList();

        // 写入 Redis 缓存
        stringRedisTemplate.opsForValue().set(
                rolesCacheKey, JSONUtil.toJsonStr(roles), rolesCacheTtlSeconds, TimeUnit.SECONDS);

        return new AuthInfo(roles);
    }

    /**
     * 根据角色标识查询权限点（页面、组件类型），返回去重后的权限点标识（permKey）集合。
     */
    private Set<String> loadPermissions(String appKey, List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptySet();
        }

        ApplicationTable app = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey).eq("status", 1));
        if (app == null) {
            return Collections.emptySet();
        }

        Set<String> merged = new LinkedHashSet<>();
        for (String roleKey : roleKeys) {
            merged.addAll(loadRolePermissions(app.getId(), roleKey));
        }
        return merged;
    }

    /**
     * 查询单个角色的权限点（页面、组件类型）的 permKey 集合，优先读取缓存，未命中则查库并写入缓存。
     */
    private Set<String> loadRolePermissions(Long appId, String roleKey) {
        String cacheKey = permissionsCachePrefix + appId + ":" + roleKey;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSONUtil.parseArray(cached).toList(String.class).stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        RoleTable role = roleMapper.selectOne(
                new QueryWrapper<RoleTable>().eq("app_id", appId).eq("role_key", roleKey));
        if (role == null) {
            return Collections.emptySet();
        }

        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new QueryWrapper<RolePermissionTable>().eq("role_id", role.getId()))
                .stream().map(RolePermissionTable::getPermissionId).distinct().toList();

        Set<String> permissions = new LinkedHashSet<>();
        if (!permissionIds.isEmpty()) {
            permissions = permissionMapper.selectList(
                            new QueryWrapper<PermissionTable>()
                                    .in("id", permissionIds)
                                    .eq("app_id", appId)
                                    .in("perm_type", 1, 2))
                    .stream()
                    .map(PermissionTable::getPermKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        stringRedisTemplate.opsForValue().set(
                cacheKey, JSONUtil.toJsonStr(permissions), permissionsCacheTtlSeconds, TimeUnit.SECONDS);
        return permissions;
    }

    private String createRefreshToken(Long masterId, Long subId, String deviceId, String appKey, String nonce) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("master_id", masterId)
                .claim("sub_id", subId)
                .claim("device_id", deviceId)
                .claim("app_key", appKey)
                .claim("nonce", nonce)
                .issuedAt(new Date(now))
                .expiration(new Date(now + TimeUnit.DAYS.toMillis(refreshTtlDays)))
                .signWith(key)
                .compact();
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        String cookieName = buildRefreshCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private RefreshTokenClaims parseRefreshToken(String refreshToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
            return new RefreshTokenClaims(
                    toLong(claims.get("master_id")),
                    toLong(claims.get("sub_id")),
                    claims.get("device_id", String.class),
                    claims.get("app_key", String.class),
                    claims.get("nonce", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException("登录凭证无效");
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private void saveRefreshToken(Long masterId, Long subId, String deviceId, String appKey, String nonce, List<String> roles) {
        // 删除同账号+设备+应用下的旧长token，避免 Redis 残留
        String pattern = keyPrefix + masterId + ":" + subId + ":" + deviceId + ":" + appKey + ":*";
        RefreshTokenRedisUtil.deleteByPattern(stringRedisTemplate, pattern);

        String key = buildRefreshTokenKey(masterId, subId, deviceId, appKey, nonce);
        RefreshTokenMeta meta = new RefreshTokenMeta(roles);
        RefreshTokenRedisUtil.save(stringRedisTemplate, key, meta, refreshTtlDays);
    }

    private String buildRefreshTokenKey(Long masterId, Long subId, String deviceId, String appKey, String nonce) {
        return keyPrefix + masterId + ":" + subId + ":" + deviceId + ":" + appKey + ":" + nonce;
    }

    private String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record RefreshTokenClaims(Long masterId, Long subId, String deviceId, String appKey, String nonce) {
    }

    private String buildRefreshCookieName() {
        return refreshCookieName + "-" + appKeyUser;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (response == null) {
            return;
        }
        Cookie cookie = new Cookie(buildRefreshCookieName(), refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(refreshTtlDays));
        response.addCookie(cookie);
    }

    private void recordLoginDevice(Long accountId, String deviceId, String refreshToken, LoginDeviceTable device) {
        LocalDateTime now = LocalDateTime.now();
        if (device == null) {
            device = new LoginDeviceTable();
            device.setAccountId(accountId);
            device.setDeviceId(deviceId);
            device.setLastLoginTime(now);
            device.setLastActiveTime(now);
            device.setStatus(1);
            loginDeviceMapper.insert(device);
        } else {
            device.setLastLoginTime(now);
            device.setLastActiveTime(now);
            device.setStatus(1);
            device.setUpdateTime(now);
            loginDeviceMapper.updateById(device);
        }
    }

    private void recordLoginLog(SubAccountTable subAccount, String deviceId, String appkey, String browser, String os,
                                String deviceType, String userAgent, String timeZone, String location,
                                String ip, int result, String failReason) {
        if (subAccount == null) {
            return;
        }
        LoginLogTable log = new LoginLogTable();
        log.setAccountId(subAccount.getId());
        log.setDeviceId(deviceId);
        log.setAppKey(appkey);
        log.setBrowser(browser);
        log.setOs(os);
        log.setDeviceType(deviceType);
        log.setUserAgent(userAgent);
        log.setTimeZone(timeZone);
        log.setLocation(location);
        log.setIp(ip);
        log.setResult(result);
        log.setFailReason(failReason);
        loginLogMapper.insert(log);
    }

    private String createAccessToken(Long subAccountId, Long masterAccountId, String deviceId, String appKey, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("sub_account_id", subAccountId)
                .claim("master_account_id", masterAccountId)
                .claim("device_id", deviceId)
                .claim("app_key", appKey)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + TimeUnit.MINUTES.toMillis(accessTtlMinutes)))
                .signWith(key)
                .compact();
    }

    private record AuthInfo(List<String> roles) {
    }

}
