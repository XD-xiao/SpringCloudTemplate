package com.example.common.utils;

import com.example.common.auth.UserIdentity;

import java.util.List;

/**
 * 当前请求登录用户的 ThreadLocal 上下文。
 *
 * <p>内部只持有一个统一的 {@link UserIdentity}，新增身份字段只需扩展该模型，
 * 无需在此增删字段。</p>
 */
public class UserContext {

    private static final ThreadLocal<UserIdentity> USER = new ThreadLocal<>();

    /**
     * 保存当前登录用户身份。
     */
    public static void set(UserIdentity identity) {
        USER.set(identity);
    }

    /**
     * 获取当前登录用户身份，可能为 {@code null}。
     */
    public static UserIdentity get() {
        return USER.get();
    }

    /**
     * 获取当前登录的马甲账号 id（sub_account_id）
     */
    public static Long getSubId() {
        UserIdentity identity = USER.get();
        return identity == null ? null : identity.userId();
    }

    /**
     * 获取当前登录设备 id（device_id）
     */
    public static String getDeviceId() {
        UserIdentity identity = USER.get();
        return identity == null ? null : identity.deviceId();
    }

    /**
     * 获取当前请求的应用标识（app_key）
     */
    public static String getAppKey() {
        UserIdentity identity = USER.get();
        return identity == null ? null : identity.appKey();
    }

    /**
     * 获取当前用户的角色集合（role_key）
     */
    public static List<String> getRoles() {
        UserIdentity identity = USER.get();
        return identity == null ? List.of() : identity.roles();
    }

    /**
     * 移除当前登录用户信息
     */
    public static void removeUser() {
        USER.remove();
    }
}
