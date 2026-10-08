package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备 Mapper（device）。
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
