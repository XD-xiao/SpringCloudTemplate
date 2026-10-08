package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录记录 Mapper（login_log）。
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
