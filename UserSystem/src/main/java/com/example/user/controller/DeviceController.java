package com.example.user.controller;

import com.example.common.domain.Result;
import com.example.user.permission.PermissionName;
import com.example.user.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/device")
public class DeviceController {

    private final DeviceService deviceService;

    @PreAuthorize("hasAuthority('device:info:list')")
    @PermissionName("获得设备信息列表")
    @GetMapping("/deviceInfo/list")
    public Result getDeviceList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String fingerprint,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) String browserName
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取设备信息列表成功",
                deviceService.getDevicePage(current, pageSize, fingerprint, deviceType, osName, browserName));
    }
}
