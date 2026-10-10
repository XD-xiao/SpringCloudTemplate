package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.LoginLog;
import com.example.common.domain.PageBean;
import com.example.user.mapper.LoginLogMapper;
import com.example.user.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@link LoginLogService} 实现。
 */
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginLog(Long userId, Long deviceId, String loginIp,
                               String timezone, BigDecimal latitude, BigDecimal longitude,
                               Integer loginType, Integer loginResult) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setDeviceId(deviceId);
        log.setLoginTime(LocalDateTime.now());
        log.setLoginIp(loginIp);
        log.setTimezone(timezone);
        log.setLatitude(latitude);
        log.setLongitude(longitude);
        log.setLoginType(loginType);
        log.setLoginResult(loginResult);
        loginLogMapper.insert(log);
    }

    @Override
    public LoginLog getLoginLogById(Long id) {
        return loginLogMapper.selectById(id);
    }

    @Override
    public PageBean<LoginLog> getLoginLogPage(long page, long size, Long userId) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(LoginLog::getUserId, userId);
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);
        Page<LoginLog> result = loginLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageBean<>(result.getTotal(), result.getRecords());
    }
}
