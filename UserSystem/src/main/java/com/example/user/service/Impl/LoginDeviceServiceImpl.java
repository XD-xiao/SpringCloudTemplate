package com.example.user.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.PageBean;
import com.example.common.domain.Entity.UserSystem.LoginDeviceTable;
import com.example.common.domain.Entity.UserSystem.LoginLogTable;
import com.example.common.domain.Entity.UserSystem.SubAccountTable;
import com.example.common.domain.vo.UserSystem.DeviceVO;
import com.example.common.domain.vo.UserSystem.LoginLogVO;
import com.example.common.exception.BusinessException;
import com.example.common.utils.RefreshTokenRedisUtil;
import com.example.user.mapper.LoginDeviceMapper;
import com.example.user.mapper.LoginLogMapper;
import com.example.user.mapper.SubAccountMapper;
import com.example.user.service.LoginDeviceService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginDeviceServiceImpl implements LoginDeviceService {

    /** 登录令牌在 Redis 中的 key 前缀 */
    @Value("${user-system.token.key-prefix:login:RefreshToken:}")
    private String keyPrefix;

    /** 刷新令牌存放的 Cookie 名称前缀（实际 Cookie 名称为 {name}-{appKey}） */
    @Value("${user-system.token.refresh-cookie-name:RefreshToken}")
    private String refreshCookieName;

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginDeviceMapper loginDeviceMapper;
    private final LoginLogMapper loginLogMapper;
    private final SubAccountMapper subAccountMapper;

    @Override
    public PageBean<DeviceVO> getDeviceListBySubId(Integer page, Integer size, Integer subId, String deviceId, String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<LoginDeviceTable> wrapper = new QueryWrapper<>();
        if (subId != null) {
            wrapper.eq("account_id", subId);
        }
        if (deviceId != null && !deviceId.isBlank()) {
            wrapper.eq("device_id", deviceId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("device_name", keyword).or().like("device_id", keyword));
        }
        wrapper.orderByDesc("last_active_time");

        Page<LoginDeviceTable> result = loginDeviceMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<DeviceVO> list = result.getRecords().stream().map(this::toDeviceVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    public void updateDeviceStatus(Integer subId, String deviceId, Integer status) {
        LoginDeviceTable device = loginDeviceMapper.selectOne(
                new QueryWrapper<LoginDeviceTable>()
                        .eq("account_id", subId)
                        .eq("device_id", deviceId));
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setStatus(status);
        device.setUpdateTime(LocalDateTime.now());
        loginDeviceMapper.updateById(device);

        // 2 停用 / 3 禁用：同时做下线处理，删除该设备对应的 Refresh Token
        if (status != null && (status == 2 || status == 3)) {
            kickDeviceTokens(subId, deviceId);
        }
    }

    @Override
    public void kickDevice(Integer masterId, Integer subId, String deviceId) {
        // key =  login:RefreshToken:{masterId}:{subId}:{deviceId}:*
        // 删除 redis 中该 key 前缀对应的所有 Refresh Token，使其立即失效
        String pattern = keyPrefix + masterId + ":" + subId + ":" + deviceId + ":*";
        RefreshTokenRedisUtil.deleteByPattern(stringRedisTemplate, pattern);
    }

    @Override
    public void clearCookie() {
        // Cookie格式:RefreshToken-{appKey} : .....
        // 清理所有系统（所有 appKey）的 RefreshToken Cookie，将其置为过期
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        if (request == null || response == null) {
            return;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName() != null && cookie.getName().startsWith(refreshCookieName + "-")) {
                Cookie expired = new Cookie(cookie.getName(), "");
                expired.setHttpOnly(true);
                expired.setPath("/");
                expired.setMaxAge(0);
                response.addCookie(expired);
            }
        }
    }

    @Override
    public boolean isMyDevice(String deviceId, Integer masterId) {
        if (deviceId == null || deviceId.isBlank() || masterId == null) {
            return false;
        }

        List<LoginDeviceTable> devices = loginDeviceMapper.selectList(
                new QueryWrapper<LoginDeviceTable>().eq("device_id", deviceId));
        if (devices.isEmpty()) {
            return false;
        }

        for (LoginDeviceTable device : devices) {
            if (device.getAccountId() == null) {
                continue;
            }
            SubAccountTable subAccount = subAccountMapper.selectById(device.getAccountId());
            if (subAccount != null
                    && subAccount.getMasterAccountId() != null
                    && subAccount.getMasterAccountId().equals(masterId.longValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteDevice(String deviceId) {
        Long count = loginDeviceMapper.selectCount(
                new QueryWrapper<LoginDeviceTable>().eq("device_id", deviceId));
        if (count == null || count == 0) {
            throw new BusinessException("设备不存在");
        }

        // 先下线：删除该 deviceId 在所有账号、所有应用下的 Refresh Token
        kickDeviceTokens(deviceId);

        // 再删除设备记录
        loginDeviceMapper.delete(new QueryWrapper<LoginDeviceTable>().eq("device_id", deviceId));
    }

    @Override
    public PageBean<LoginLogVO> getLogsList(Integer page, Integer size, Integer subId, String deviceId, String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<LoginLogTable> wrapper = new QueryWrapper<>();
        if (subId != null) {
            wrapper.eq("account_id", subId);
        }
        if (deviceId != null && !deviceId.isBlank()) {
            wrapper.eq("device_id", deviceId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("device_name", keyword)
                    .or().like("browser", keyword)
                    .or().like("os", keyword)
                    .or().like("ip", keyword));
        }
        wrapper.orderByDesc("id");

        Page<LoginLogTable> result = loginLogMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<LoginLogVO> list = result.getRecords().stream().map(this::toLogVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    // ==================== 辅助方法 ====================

    private DeviceVO toDeviceVO(LoginDeviceTable device) {
        return new DeviceVO(
                device.getId(),
                device.getDeviceId(),
                device.getDeviceName(),
                device.getLastLoginTime(),
                device.getLastActiveTime(),
                device.getStatus());
    }

    private LoginLogVO toLogVO(LoginLogTable log) {
        return new LoginLogVO(
                log.getId(),
                log.getAccountId(),
                log.getDeviceId(),
                log.getDeviceName(),
                log.getAppKey(),
                log.getBrowser(),
                log.getOs(),
                log.getDeviceType(),
                log.getTimeZone(),
                log.getLocation(),
                log.getIp(),
                log.getIpRegion(),
                log.getResult(),
                log.getFailReason(),
                log.getCreateTime());
    }

    private void kickDeviceTokens(Integer subId, String deviceId) {
        // 通过通配符覆盖该马甲账号该设备下的所有 Refresh Token（masterId 取通配）
        String pattern = keyPrefix + "*:" + subId + ":" + deviceId + ":*";
        RefreshTokenRedisUtil.deleteByPattern(stringRedisTemplate, pattern);
    }

    private void kickDeviceTokens(String deviceId) {
        // 删除该 deviceId 在所有账号、所有应用下的 Refresh Token
        String pattern = keyPrefix + "*:*:" + deviceId + ":*";
        RefreshTokenRedisUtil.deleteByPattern(stringRedisTemplate, pattern);
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
}
