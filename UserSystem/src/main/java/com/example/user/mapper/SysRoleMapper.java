package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper（sys_role）。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
