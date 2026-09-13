package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.MasterAccountTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MasterAccountMapper extends BaseMapper<MasterAccountTable> {
}
