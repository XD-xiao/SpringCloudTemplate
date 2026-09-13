package com.example.common.utils;

import java.util.List;

public class UserContext {

    private static final ThreadLocal<Integer> SUB_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> MASTER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> DEVICE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> APP_KEY = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ROLES = new ThreadLocal<>();

    /**
     * 保存当前登录用户信息到 ThreadLocal
     */
    public static void setUser(Integer subId, Integer masterId, String deviceId, String appKey, List<String> roles) {
        SUB_ID.set(subId);
        MASTER_ID.set(masterId);
        DEVICE_ID.set(deviceId);
        APP_KEY.set(appKey);
        ROLES.set(roles);
    }

    /**
     * 获取当前登录的马甲账号 id（sub_account_id）
     */
    public static Integer getSubId() {
        return SUB_ID.get();
    }

    /**
     * 获取当前登录的主账号 id（master_account_id）
     */
    public static Integer getMasterId() {
        return MASTER_ID.get();
    }

    /**
     * 获取当前登录设备 id（device_id）
     */
    public static String getDeviceId() {
        return DEVICE_ID.get();
    }

    /**
     * 获取当前请求的应用标识（app_key）
     */
    public static String getAppKey() {
        return APP_KEY.get();
    }

    /**
     * 获取当前用户的角色集合（role_key）
     */
    public static List<String> getRoles() {
        return ROLES.get();
    }

    /**
     * 移除当前登录用户信息
     */
    public static void removeUser() {
        SUB_ID.remove();
        MASTER_ID.remove();
        DEVICE_ID.remove();
        APP_KEY.remove();
        ROLES.remove();
    }
}
