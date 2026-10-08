package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 Mapper（user_profile）。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
