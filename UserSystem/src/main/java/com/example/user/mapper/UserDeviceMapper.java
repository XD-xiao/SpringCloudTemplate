package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.UserDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-设备关联 Mapper（user_device）。
 */
@Mapper
public interface UserDeviceMapper extends BaseMapper<UserDevice> {
}
