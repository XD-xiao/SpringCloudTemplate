package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.SysPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限点 Mapper（sys_permission）。
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
}
