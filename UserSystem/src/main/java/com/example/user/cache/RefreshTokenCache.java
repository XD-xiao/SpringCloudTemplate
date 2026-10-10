package com.example.user.cache;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.utils.RefreshTokenRedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录会话缓存（只访问 Redis）。
 *
 * <p>key：{@code auth:session:{userId}:{deviceId}}；value 为 {@link RefreshSession} 的 JSON；
 * TTL 为 {@code jwt.refresh-ttl}（滑动）。「读取-比对-轮换」用 Lua 脚本原子执行，避免并发竞态。</p>
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCache {

    private static final String SESSION_PREFIX = "auth:session:";

    /**
     * Lua 原子轮换脚本：GET + activeJti 比对 + SET 在同一脚本内完成。
     * 返回 1 成功；-1 会话不存在；-2 重放（旧 jti 再次使用，会话已删除）。
     */
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n" +
            "local expectedJti = ARGV[1]\n" +
            "local newJti = ARGV[2]\n" +
            "local newRolesJson = ARGV[3]\n" +
            "local ttlSeconds = tonumber(ARGV[4])\n" +
            "local json = redis.call('GET', key)\n" +
            "if not json then\n" +
            "  return -1\n" +
            "end\n" +
            "local ok, session = pcall(cjson.decode, json)\n" +
            "if not ok or type(session) ~= 'table' or session['activeJti'] ~= expectedJti then\n" +
            "  redis.call('DEL', key)\n" +
            "  return -2\n" +
            "end\n" +
            "session['activeJti'] = newJti\n" +
            "local rolesOk, roles = pcall(cjson.decode, newRolesJson)\n" +
            "if rolesOk and type(roles) == 'table' then\n" +
            "  session['roles'] = roles\n" +
            "end\n" +
            "redis.call('SET', key, cjson.encode(session), 'EX', ttlSeconds)\n" +
            "return 1",
            Long.class);

    private final StringRedisTemplate redis;

    /**
     * 写入会话（登录）。
     */
    public void saveSession(Long userId, String deviceId, List<String> roles, String activeJti, long ttlSeconds) {
        JSONObject obj = new JSONObject();
        // userId 存为字符串，避免 Redis Lua cjson 对雪花 ID（>2^53）做浮点转储导致精度丢失
        obj.set("userId", userId == null ? null : String.valueOf(userId));
        obj.set("deviceId", deviceId);
        obj.set("roles", roles == null ? List.of() : roles);
        obj.set("activeJti", activeJti);
        redis.opsForValue().set(sessionKey(userId, deviceId), obj.toString(), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 读取会话；不存在或数据不完整时返回 {@code null}。
     */
    public RefreshSession getSession(Long userId, String deviceId) {
        String json = redis.opsForValue().get(sessionKey(userId, deviceId));
        if (json == null || json.isBlank()) {
            return null;
        }
        JSONObject obj = JSONUtil.parseObj(json);
        Object userIdVal = obj.get("userId");
        Long uId = userIdVal == null ? null : Long.valueOf(String.valueOf(userIdVal));
        String dId = obj.getStr("deviceId");
        JSONArray rolesArr = obj.getJSONArray("roles");
        List<String> roles = rolesArr == null ? List.of() : rolesArr.toList(String.class);
        String activeJti = obj.getStr("activeJti");
        return new RefreshSession(uId, dId, roles, activeJti);
    }

    /**
     * 删除单个设备会话（登出 / 禁用设备）。
     */
    public void deleteByDevice(Long userId, String deviceId) {
        redis.delete(sessionKey(userId, deviceId));
    }

    /**
     * 按前缀删除该用户全部设备会话（禁用用户）。
     */
    public long deleteByUser(Long userId) {
        return RefreshTokenRedisUtil.deleteByPattern(redis, sessionKey(userId, "*"));
    }

    /**
     * Lua 原子轮换会话的 activeJti 与 roles，并重置 TTL。
     *
     * @return 1 成功；-1 会话不存在；-2 重放（旧 jti 再次使用，会话已删除）
     */
    public long rotate(Long userId, String deviceId, String expectedJti, String newJti,
                       List<String> newRoles, long ttlSeconds) {
        Long result = redis.execute(ROTATE_SCRIPT,
                List.of(sessionKey(userId, deviceId)),
                expectedJti,
                newJti,
                JSONUtil.toJsonStr(newRoles == null ? List.of() : newRoles),
                String.valueOf(ttlSeconds));
        return result == null ? -1 : result;
    }

    private String sessionKey(Long userId, String deviceId) {
        return SESSION_PREFIX + userId + ":" + deviceId;
    }
}
