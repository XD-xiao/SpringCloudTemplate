package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Long id;
    private String roleName;
    private String roleKey;
}
