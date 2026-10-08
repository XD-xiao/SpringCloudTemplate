package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（sys_user）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
