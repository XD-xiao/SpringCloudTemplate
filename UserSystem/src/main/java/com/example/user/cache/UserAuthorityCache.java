package com.example.user.cache;

import cn.hutool.json.JSONUtil;
import com.example.common.utils.RefreshTokenRedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RBAC 权限数据缓存（两级，无 TTL 持久存储）。
 *
 * <p>把 RBAC 五张表（用户、角色、权限、用户-角色关联、角色-权限关联）的授权结果
 * 统一抽象为两级缓存，供鉴权过滤器高频读取：</p>
 * <ul>
 *     <li>用户→角色：key {@code auth:user:{userId}}，value 为角色 ID 列表；</li>
 *     <li>角色→权限：key {@code auth:role:{roleId}}，value 为权限标识（perm_key）列表。</li>
 * </ul>
 *
 * <p>缓存采用 Cache-Aside 模式：读取未命中时由业务层查库并回填；
 * 用户-角色 / 角色 / 角色-权限 / 权限任一变更时，通过 {@link #evictUser} /
 * {@link #evictRole} / {@link #evictAll} 主动失效，保证与数据库一致。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAuthorityCache {

    private static final String USER_ROLES_PREFIX = "auth:user:";
    private static final String ROLE_PERMS_PREFIX = "auth:role:";

    private final StringRedisTemplate redis;

    /**
     * 读取用户角色 ID 列表；缓存不存在返回 {@code null}，以区分「已缓存的空集合」。
     */
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return null;
        }
        String json = redis.opsForValue().get(userRolesKey(userId));
        if (json == null) {
            return null;
        }
        return JSONUtil.parseArray(json).toList(Long.class);
    }

    /**
     * 写入用户角色 ID 列表。
     */
    public void putRoleIds(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }
        List<Long> values = roleIds == null ? List.of() : roleIds;
        redis.opsForValue().set(userRolesKey(userId), JSONUtil.toJsonStr(values));
    }

    /**
     * 读取角色权限标识列表；缓存不存在返回 {@code null}，以区分「已缓存的空集合」。
     */
    public List<String> getPermKeys(Long roleId) {
        if (roleId == null) {
            return null;
        }
        String json = redis.opsForValue().get(rolePermsKey(roleId));
        if (json == null) {
            return null;
        }
        return JSONUtil.parseArray(json).toList(String.class);
    }

    /**
     * 写入角色权限标识列表。
     */
    public void putPermKeys(Long roleId, List<String> permKeys) {
        if (roleId == null) {
            return;
        }
        List<String> values = permKeys == null ? List.of() : permKeys;
        redis.opsForValue().set(rolePermsKey(roleId), JSONUtil.toJsonStr(values));
    }

    /**
     * 失效指定用户的角色缓存（用户-角色关系变更时使用）。
     */
    public void evictUser(Long userId) {
        if (userId != null) {
            redis.delete(userRolesKey(userId));
        }
    }

    /**
     * 失效指定角色的权限缓存（角色-权限关系变更时使用）。
     */
    public void evictRole(Long roleId) {
        if (roleId != null) {
            redis.delete(rolePermsKey(roleId));
        }
    }

    /**
     * 失效全部 RBAC 缓存（角色/权限本身变更、权限点同步时使用）。
     */
    public void evictAll() {
        RefreshTokenRedisUtil.deleteByPattern(redis, USER_ROLES_PREFIX + "*");
        RefreshTokenRedisUtil.deleteByPattern(redis, ROLE_PERMS_PREFIX + "*");
    }

    private String userRolesKey(Long userId) {
        return USER_ROLES_PREFIX + userId;
    }

    private String rolePermsKey(Long roleId) {
        return ROLE_PERMS_PREFIX + roleId;
    }
}
