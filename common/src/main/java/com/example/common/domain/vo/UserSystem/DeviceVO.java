package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVO {

    private Long id;
    private String deviceId;
    private String deviceName;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastActiveTime;
    private Integer status;
}
