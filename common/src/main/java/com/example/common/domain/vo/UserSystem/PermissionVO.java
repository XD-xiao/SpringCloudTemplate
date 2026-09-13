package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    private Long id;
    private String permName;
    private String permKey;
    private Integer permType;
    private String path;
}
