package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联 Mapper（sys_user_role）。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
