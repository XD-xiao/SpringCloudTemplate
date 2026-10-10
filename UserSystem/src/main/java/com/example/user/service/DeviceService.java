package com.example.user.service;

import com.example.common.domain.Entity.UserSystem.Device;
import com.example.common.domain.Entity.UserSystem.UserDevice;
import com.example.common.domain.PageBean;
import com.example.user.vo.UserDeviceVO;

/**
 * 设备域业务接口。
 *
 * <p>覆盖设备（device）与用户-设备关联（user_device）两类数据：</p>
 * <ul>
 *     <li>登录记录：按指纹记录设备信息（新设备新增、旧设备更新），并维护用户-设备关联；</li>
 *     <li>设备管理：设备与用户设备的启用/禁用；</li>
 *     <li>设备查询：设备、用户设备的分页查询。</li>
 * </ul>
 */
public interface DeviceService {

    /**
     * 登录时记录设备信息。
     *
     * <p>按指纹（fingerprint）查询设备：不存在则新增，存在则更新为最新软硬件信息。</p>
     *
     * @return 新增或更新后的设备（含 {@code id}）
     */
    Device recordDevice(String fingerprint, String deviceType, String deviceCategory,
                        String osName, String osVersion, String browserName, String browserVersion,
                        String userAgent);

    /**
     * 新增或更新用户-设备关联。
     *
     * <p>关联不存在则新增（默认启用），存在则刷新最近登录时间；不改变既有启用/禁用状态。</p>
     */
    void bindUserDevice(Long userId, Long deviceId);

    /**
     * 按指纹查询设备（唯一性校验用）。
     */
    Device getDeviceByFingerprint(String fingerprint);

    /**
     * 按用户 + 设备查询关联（登录时校验设备状态用）。
     */
    UserDevice getUserDevice(Long userId, Long deviceId);

    /**
     * 分页查询设备（可按指纹 / 设备类型 / 操作系统 / 浏览器过滤）。
     */
    PageBean<Device> getDevicePage(long page, long size, String fingerprint,
                                   String deviceType, String osName, String browserName);

    /**
     * 分页查询用户设备（可按用户、指纹、状态过滤），返回关联表状态与设备详情合并后的 VO。
     */
    PageBean<UserDeviceVO> getUserDevicePage(long page, long size, Long userId,
                                             String fingerprint, Integer status);

    /**
     * 启用/禁用用户设备（按用户 + 设备定位关联）。
     */
    void updateUserDeviceStatus(Long userId, Long deviceId, Integer status);

    /**
     * 启用/禁用本人设备（按关联表主键定位，并校验归属）。
     *
     * @param userId       当前登录用户 ID
     * @param userDeviceId 用户-设备关联 ID（user_device.id）
     */
    void updateUserDeviceStatusById(Long userId, Long userDeviceId, Integer status);
}
