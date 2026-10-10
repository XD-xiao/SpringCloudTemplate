package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.Device;
import com.example.common.domain.Entity.UserSystem.UserDevice;
import com.example.common.domain.PageBean;
import com.example.common.exception.BusinessException;
import com.example.user.cache.RefreshTokenCache;
import com.example.user.mapper.DeviceMapper;
import com.example.user.mapper.UserDeviceMapper;
import com.example.user.service.DeviceService;
import com.example.user.vo.UserDeviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link DeviceService} 实现。
 *
 * <p>设备（device）与用户-设备关联（user_device）的维护均在此实现。</p>
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final RefreshTokenCache refreshTokenCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device recordDevice(String fingerprint, String deviceType, String deviceCategory,
                               String osName, String osVersion, String browserName, String browserVersion,
                               String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        Device device = getDeviceByFingerprint(fingerprint);

        if (device == null) {
            // 新设备：新增
            device = new Device();
            device.setFingerprint(fingerprint);
            device.setCreateTime(now);
            fillDeviceInfo(device, deviceType, deviceCategory, osName, osVersion, browserName, browserVersion, userAgent);
            device.setUpdateTime(now);
            deviceMapper.insert(device);
        } else {
            // 旧设备：更新为最新软硬件信息
            fillDeviceInfo(device, deviceType, deviceCategory, osName, osVersion, browserName, browserVersion, userAgent);
            device.setUpdateTime(now);
            deviceMapper.updateById(device);
        }
        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindUserDevice(Long userId, Long deviceId) {
        LocalDateTime now = LocalDateTime.now();
        UserDevice relation = getUserDevice(userId, deviceId);

        if (relation == null) {
            // 新关联：默认启用
            relation = new UserDevice();
            relation.setUserId(userId);
            relation.setDeviceId(deviceId);
            relation.setStatus(1);
            relation.setLastLoginTime(now);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            userDeviceMapper.insert(relation);
        } else {
            // 旧关联：仅刷新最近登录时间，不改变启用/禁用状态
            relation.setLastLoginTime(now);
            relation.setUpdateTime(now);
            userDeviceMapper.updateById(relation);
        }
    }

    @Override
    public Device getDeviceByFingerprint(String fingerprint) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getFingerprint, fingerprint));
    }

    @Override
    public UserDevice getUserDevice(Long userId, Long deviceId) {
        return userDeviceMapper.selectOne(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getDeviceId, deviceId));
    }

    @Override
    public PageBean<Device> getDevicePage(long page, long size, String fingerprint,
                                          String deviceType, String osName, String browserName) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(fingerprint != null && !fingerprint.isBlank(), Device::getFingerprint, fingerprint)
                .eq(deviceType != null && !deviceType.isBlank(), Device::getDeviceType, deviceType)
                .eq(osName != null && !osName.isBlank(), Device::getOsName, osName)
                .eq(browserName != null && !browserName.isBlank(), Device::getBrowserName, browserName)
                .orderByDesc(Device::getUpdateTime);
        Page<Device> result = deviceMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageBean<>(result.getTotal(), result.getRecords());
    }

    @Override
    public PageBean<UserDeviceVO> getUserDevicePage(long page, long size, Long userId,
                                                    String fingerprint, Integer status) {
        // 1. 指纹在 device 表上，先解析出对应 deviceId
        Long deviceId = null;
        if (fingerprint != null && !fingerprint.isBlank()) {
            Device device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getFingerprint, fingerprint));
            if (device == null) {
                return new PageBean<>(0L, List.of());
            }
            deviceId = device.getId();
        }

        // 2. 分页查询 user_device
        LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, UserDevice::getUserId, userId)
                .eq(status != null, UserDevice::getStatus, status)
                .eq(deviceId != null, UserDevice::getDeviceId, deviceId)
                .orderByDesc(UserDevice::getLastLoginTime);
        Page<UserDevice> result = userDeviceMapper.selectPage(new Page<>(page, size), wrapper);

        // 3. 批量加载设备信息并组装 VO
        List<Long> deviceIds = result.getRecords().stream()
                .map(UserDevice::getDeviceId)
                .distinct()
                .toList();
        Map<Long, Device> deviceMap = deviceIds.isEmpty()
                ? Map.of()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                        .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));

        List<UserDeviceVO> list = result.getRecords().stream()
                .map(ud -> toUserDeviceVO(ud, deviceMap.get(ud.getDeviceId())))
                .toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserDeviceStatus(Long userId, Long deviceId, Integer status) {
        UserDevice relation = getUserDevice(userId, deviceId);
        if (relation == null) {
            return;
        }
        relation.setStatus(status);
        relation.setUpdateTime(LocalDateTime.now());
        userDeviceMapper.updateById(relation);

        // 禁用设备：删除对应会话，使该设备的长 Token 立即失效（符合 C4/C5）
        if (Integer.valueOf(0).equals(status)) {
            Device device = deviceMapper.selectById(deviceId);
            if (device != null && device.getFingerprint() != null) {
                refreshTokenCache.deleteByDevice(userId, device.getFingerprint());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserDeviceStatusById(Long userId, Long userDeviceId, Integer status) {
        UserDevice relation = userDeviceMapper.selectById(userDeviceId);
        if (relation == null || !relation.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该设备");
        }
        updateUserDeviceStatus(relation.getUserId(), relation.getDeviceId(), status);
    }

    private UserDeviceVO toUserDeviceVO(UserDevice relation, Device device) {
        UserDeviceVO vo = new UserDeviceVO();
        vo.setId(relation.getId());
        vo.setDeviceId(relation.getDeviceId());
        vo.setStatus(relation.getStatus());
        vo.setLastLoginTime(relation.getLastLoginTime());
        if (device != null) {
            vo.setFingerprint(device.getFingerprint());
            vo.setDeviceType(device.getDeviceType());
            vo.setDeviceCategory(device.getDeviceCategory());
            vo.setOsName(device.getOsName());
            vo.setOsVersion(device.getOsVersion());
            vo.setBrowserName(device.getBrowserName());
            vo.setBrowserVersion(device.getBrowserVersion());
            vo.setUserAgent(device.getUserAgent());
        }
        return vo;
    }

    private void fillDeviceInfo(Device device, String deviceType, String deviceCategory,
                                String osName, String osVersion, String browserName, String browserVersion,
                                String userAgent) {
        device.setDeviceType(deviceType);
        device.setDeviceCategory(deviceCategory);
        device.setOsName(osName);
        device.setOsVersion(osVersion);
        device.setBrowserName(browserName);
        device.setBrowserVersion(browserVersion);
        device.setUserAgent(userAgent);
    }
}
