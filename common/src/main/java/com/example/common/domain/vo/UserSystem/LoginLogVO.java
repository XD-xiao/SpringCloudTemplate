package com.example.common.domain.vo.UserSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogVO {

    private Long id;
    private Long accountId;
    private String deviceId;
    private String deviceName;
    private String appKey;
    private String browser;
    private String os;
    private String deviceType;
    private String timeZone;
    private String location;
    private String ip;
    private String ipRegion;
    private Integer result;
    private String failReason;
    private LocalDateTime createTime;
}
