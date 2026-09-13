package com.example.user.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.PageBean;
import com.example.common.domain.Entity.UserSystem.MasterAccountTable;
import com.example.common.domain.Entity.UserSystem.SubAccountRoleTable;
import com.example.common.domain.Entity.UserSystem.SubAccountTable;
import com.example.common.domain.vo.UserSystem.AuthCodeVO;
import com.example.common.domain.vo.UserSystem.MasterInfoVO;
import com.example.common.domain.vo.UserSystem.SubAccountVO;
import com.example.common.exception.BusinessException;
import com.example.common.utils.RefreshTokenRedisUtil;
import com.example.common.utils.UserContext;
import com.example.user.mapper.MasterAccountMapper;
import com.example.user.mapper.SubAccountMapper;
import com.example.user.mapper.SubAccountRoleMapper;
import com.example.user.service.AccountService;
import com.example.user.service.LoginDeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    /** 授权码在 Redis 中的 key 前缀 */
    @Value("${user-system.auth-code.redis-prefix:auth:code:}")
    private String authCodePrefix;

    /** 授权码有效期（秒） */
    @Value("${user-system.auth-code.ttl-seconds:50}")
    private long codeTtlSeconds;

    /** 授权码随机 TTL 抖动上限（秒） */
    @Value("${user-system.auth-code.ttl-jitter-seconds:20}")
    private long codeTtlJitterSeconds;

    /** 生成授权码场景使用的类型标识 */
    @Value("${user-system.auth-code.authorize-type:authorize}")
    private String authorizeType;

    /** 应用标识（app_key）兜底值 */
    @Value("${user-system.app-key:UserSystem}")
    private String appKeyUser;

    /** 验证码在 Redis 中的 key 前缀 */
    @Value("${user-system.verify-code.redis-prefix:verify:code:}")
    private String verifyCodePrefix;

    /** 注册场景使用的验证码类型标识（创建马甲账号复用） */
    @Value("${user-system.verify-code.register-type:register}")
    private String registerType;

    /** 登录令牌在 Redis 中的 key 前缀 */
    @Value("${user-system.token.key-prefix:login:RefreshToken:}")
    private String tokenKeyPrefix;

    /** 角色缓存 key 前缀（与登录鉴权缓存保持一致，用于失效同步） */
    @Value("${user-system.roles-cache.redis-prefix:auth:roles:}")
    private String rolesCachePrefix;

    /** 主账号信息缓存 key 前缀 */
    @Value("${user-system.account-cache.master-prefix:account:master:}")
    private String masterCachePrefix;

    /** 马甲账号信息缓存 key 前缀 */
    @Value("${user-system.account-cache.sub-prefix:account:sub:}")
    private String subCachePrefix;

    /** 缓存基础有效期（秒） */
    @Value("${user-system.account-cache.ttl-seconds:300}")
    private long cacheTtlSeconds;

    /** 缓存随机 TTL 抖动上限（秒） */
    @Value("${user-system.account-cache.ttl-jitter-seconds:120}")
    private long cacheTtlJitterSeconds;

    /** 缓存空值有效期（秒） */
    @Value("${user-system.account-cache.empty-ttl-seconds:60}")
    private long emptyTtlSeconds;

    /** 缓存空值占位符 */
    @Value("${user-system.account-cache.empty-value:__EMPTY__}")
    private String emptyValue;

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginDeviceService loginDeviceService;

    private final MasterAccountMapper masterAccountMapper;
    private final SubAccountMapper subAccountMapper;
    private final SubAccountRoleMapper subAccountRoleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AuthCodeVO createAuthorize(String email, String appKey) {
        String redisKey = buildAuthCodeKey(appKey, email);

        // 已存在授权码则直接返回剩余有效时间，避免重复生成
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode != null) {
            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            return new AuthCodeVO(cachedCode, ttl == null || ttl < 0 ? 0L : ttl);
        }

        // 生成随机 6 位数字授权码，并写入随机 TTL，避免缓存雪崩
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        long ttl = randomTtl(codeTtlSeconds, codeTtlJitterSeconds);
        stringRedisTemplate.opsForValue().set(redisKey, code, ttl, TimeUnit.SECONDS);

        System.out.println(email + " : " + code);

        return new AuthCodeVO(code, ttl);
    }

    @Override
    public void logout() {
        Integer masterId = UserContext.getMasterId();
        Integer subId = UserContext.getSubId();
        String deviceId = UserContext.getDeviceId();

        loginDeviceService.clearCookie();
        loginDeviceService.kickDevice(masterId, subId, deviceId);
    }

    @Override
    @Transactional
    public Long createSub(String email, String code, String aliasName) {
        String appKey = currentAppKey();
        String redisKey = verifyCodePrefix + appKey + ":" + registerType + ":" + email;

        // 校验邮箱验证码，但先不删除，待所有校验通过后再消费，避免校验失败时误吞验证码
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException("验证码错误或已失效");
        }

        // 邮箱全局唯一校验
        Long emailCount = subAccountMapper.selectCount(
                new QueryWrapper<SubAccountTable>().eq("email", email));
        if (emailCount != null && emailCount > 0) {
            throw new BusinessException("该邮箱已被使用");
        }

        Integer masterId = UserContext.getMasterId();
        if (masterId == null) {
            throw new BusinessException("无法确定当前主账号");
        }

        // 校验通过后删除验证码，防止复用
        stringRedisTemplate.delete(redisKey);

        SubAccountTable subAccount = new SubAccountTable();
        subAccount.setEmail(email);
        subAccount.setMasterAccountId(masterId.longValue());
        subAccount.setAliasName(aliasName);
        subAccount.setStatus(0);
        subAccountMapper.insert(subAccount);

        return subAccount.getId();
    }

    @Override
    public PageBean<MasterInfoVO> getMasters(Integer page, Integer size, String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<MasterAccountTable> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("id_card", keyword);
        }
        wrapper.orderByDesc("id");

        Page<MasterAccountTable> result = masterAccountMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<MasterInfoVO> list = result.getRecords().stream().map(this::toMasterInfoVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    public MasterInfoVO getMasterInfo() {
        Integer masterId = UserContext.getMasterId();
        if (masterId == null) {
            throw new BusinessException("无法确定当前主账号");
        }

        MasterInfoVO info = getMasterInfoCache(masterId);
        if (info == null) {
            throw new BusinessException("主账号不存在");
        }
        return info;
    }

    @Override
    public void updateMasterStatus(Integer masterId, Integer status) {
        MasterAccountTable master = masterAccountMapper.selectById(masterId.longValue());
        if (master == null) {
            throw new BusinessException("主账号不存在");
        }

        master.setStatus(status);
        masterAccountMapper.updateById(master);

        // 同步失效主账号信息缓存
        evictMasterCache(masterId);
    }

    @Override
    public PageBean<SubAccountVO> getSubListByMasterId(Integer page, Integer size, String keyword, Integer masterId) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<SubAccountTable> wrapper = new QueryWrapper<>();
        if (masterId != null) {
            wrapper.eq("master_account_id", masterId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("email", keyword).or().like("alias_name", keyword));
        }
        wrapper.orderByDesc("id");

        Page<SubAccountTable> result = subAccountMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<SubAccountVO> list = result.getRecords().stream().map(this::toSubAccountVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    public void updateSub(Integer subId, String aliasName) {
        SubAccountTable subAccount = getSubAccountCache(subId);
        if (subAccount == null) {
            throw new BusinessException("马甲账号不存在");
        }

        subAccount.setAliasName(aliasName);
        subAccountMapper.updateById(subAccount);

        evictSubCache(subId);
    }

    @Override
    public void updateSubStatus(Integer subId, Integer status) {
        SubAccountTable subAccount = getSubAccountCache(subId);
        if (subAccount == null) {
            throw new BusinessException("马甲账号不存在");
        }

        subAccount.setStatus(status);
        subAccountMapper.updateById(subAccount);

        evictSubCache(subId);
    }

    @Override
    public boolean isMySub(Integer masterId, Integer subId) {
        if (masterId == null || subId == null) {
            return false;
        }
        SubAccountTable subAccount = getSubAccountCache(subId);
        return subAccount != null
                && subAccount.getMasterAccountId() != null
                && subAccount.getMasterAccountId().equals(masterId.longValue());
    }

    @Override
    public void kickSub(Integer subId) {
        SubAccountTable subAccount = getSubAccountCache(subId);
        if (subAccount == null) {
            throw new BusinessException("马甲账号不存在");
        }

        // 删除该马甲账号在所有设备、所有应用下的 Refresh Token，使其立即下线
        Long masterId = subAccount.getMasterAccountId();
        String pattern = tokenKeyPrefix + masterId + ":" + subId + ":*";
        RefreshTokenRedisUtil.deleteByPattern(stringRedisTemplate, pattern);
    }

    @Override
    @Transactional
    public void updateSubRole(Integer subId, List<Long> roleIds) {
        SubAccountTable subAccount = getSubAccountCache(subId);
        if (subAccount == null) {
            throw new BusinessException("马甲账号不存在");
        }

        // 覆盖写入：先删除该马甲账号已有角色，再批量插入
        subAccountRoleMapper.delete(new QueryWrapper<SubAccountRoleTable>().eq("sub_account_id", subId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SubAccountRoleTable relation = new SubAccountRoleTable();
                relation.setSubAccountId(subId.longValue());
                relation.setRoleId(roleId);
                subAccountRoleMapper.insert(relation);
            }
        }

        // 同步失效角色缓存，避免后续登录鉴权继续使用旧角色
        evictRolesCache(subId);
    }

    @Override
    public List<Long> getRoleListBySub(Long subId) {
        SubAccountTable subAccount = subAccountMapper.selectById(subId);
        if (subAccount == null) {
            throw new BusinessException("马甲账号不存在");
        }

        return subAccountRoleMapper.selectList(
                        new QueryWrapper<SubAccountRoleTable>().eq("sub_account_id", subId))
                .stream().map(SubAccountRoleTable::getRoleId).toList();
    }

    // ==================== 缓存辅助方法 ====================

    private String buildAuthCodeKey(String appKey, String email) {
        return authCodePrefix + appKey + ":" + authorizeType + ":" + email;
    }

    private String currentAppKey() {
        String appKey = UserContext.getAppKey();
        return appKey == null || appKey.isBlank() ? appKeyUser : appKey;
    }

    private long randomTtl(long baseSeconds, long jitterSeconds) {
        if (jitterSeconds <= 0) {
            return baseSeconds;
        }
        return baseSeconds + ThreadLocalRandom.current().nextLong(jitterSeconds + 1);
    }

    private MasterInfoVO getMasterInfoCache(Integer masterId) {
        String key = masterCachePrefix + masterId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (emptyValue.equals(cached)) {
                return null;
            }
            MasterInfoVO value = fromJson(cached, MasterInfoVO.class);
            if (value != null) {
                return value;
            }
            // 缓存内容异常，删除后回源
            stringRedisTemplate.delete(key);
        }

        MasterAccountTable master = masterAccountMapper.selectById(masterId.longValue());
        if (master == null) {
            // 缓存空值，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, emptyValue, emptyTtlSeconds, TimeUnit.SECONDS);
            return null;
        }

        MasterInfoVO info = toMasterInfoVO(master);
        String json = toJson(info);
        if (json != null) {
            stringRedisTemplate.opsForValue().set(key, json, randomTtl(cacheTtlSeconds, cacheTtlJitterSeconds), TimeUnit.SECONDS);
        }
        return info;
    }

    private SubAccountTable getSubAccountCache(Integer subId) {
        String key = subCachePrefix + subId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (emptyValue.equals(cached)) {
                return null;
            }
            SubAccountTable value = fromJson(cached, SubAccountTable.class);
            if (value != null) {
                return value;
            }
            stringRedisTemplate.delete(key);
        }

        SubAccountTable subAccount = subAccountMapper.selectById(subId.longValue());
        if (subAccount == null) {
            stringRedisTemplate.opsForValue().set(key, emptyValue, emptyTtlSeconds, TimeUnit.SECONDS);
            return null;
        }

        String json = toJson(subAccount);
        if (json != null) {
            stringRedisTemplate.opsForValue().set(key, json, randomTtl(cacheTtlSeconds, cacheTtlJitterSeconds), TimeUnit.SECONDS);
        }
        return subAccount;
    }

    private void evictMasterCache(Integer masterId) {
        stringRedisTemplate.delete(masterCachePrefix + masterId);
    }

    private void evictSubCache(Integer subId) {
        stringRedisTemplate.delete(subCachePrefix + subId);
    }

    private void evictRolesCache(Integer subId) {
        stringRedisTemplate.delete(rolesCachePrefix + currentAppKey() + ":" + subId);
    }

    private MasterInfoVO toMasterInfoVO(MasterAccountTable master) {
        return new MasterInfoVO(
                master.getId(),
                maskIdCard(master.getIdCard()),
                master.getStatus(),
                master.getCreateTime(),
                master.getUpdateTime());
    }

    private SubAccountVO toSubAccountVO(SubAccountTable subAccount) {
        return new SubAccountVO(
                subAccount.getId(),
                subAccount.getEmail(),
                subAccount.getAliasName(),
                subAccount.getStatus(),
                subAccount.getCreateTime(),
                subAccount.getUpdateTime());
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }
}
