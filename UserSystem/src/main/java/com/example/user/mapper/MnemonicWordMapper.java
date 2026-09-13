package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.domain.Entity.UserSystem.MnemonicWordTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MnemonicWordMapper extends BaseMapper<MnemonicWordTable> {
}
