package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubAccountVO {

    private Long id;
    private String email;
    private String aliasName;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
