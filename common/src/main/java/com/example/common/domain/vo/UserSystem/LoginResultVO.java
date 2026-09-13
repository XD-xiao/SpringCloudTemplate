package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {

    /** 访问令牌（已拼接 Bearer 前缀） */
    private String token;

    /** 权限点集合（permKey，仅页面、组件类型） */
    private Set<String> permissions;
}
