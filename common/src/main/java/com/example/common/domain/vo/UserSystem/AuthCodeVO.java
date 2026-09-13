package com.example.common.domain.vo.UserSystem;

import lombok.Data;

/**
 * 授权码返回对象
 */
@Data
public class AuthCodeVO {

    /** 授权码内容 */
    private String code;

    /** 剩余有效时间（秒） */
    private Long ttl;

    public AuthCodeVO(String code, Long ttl) {
        this.code = code;
        this.ttl = ttl;
    }
}
