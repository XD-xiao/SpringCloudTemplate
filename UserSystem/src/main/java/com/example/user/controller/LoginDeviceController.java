package com.example.user.controller;

import com.example.common.domain.Result;
import com.example.common.exception.BusinessException;
import com.example.common.utils.UserContext;
import com.example.user.permission.PermissionName;
import com.example.user.service.LoginDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice")
public class LoginDeviceController {

    private final LoginDeviceService loginDeviceService;

    @PreAuthorize("hasAuthority('user:device:list:me')")
    @PermissionName("获得本人登录设备列表")
    @GetMapping("/devices/me")
    public Result getMyDeviceList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String keyword
    ){

        Integer subId = UserContext.getSubId();

        return Result.success("获得本人登录设备列表成功" , loginDeviceService.getDeviceListBySubId(page, size, subId, deviceId, keyword));
    }

    @PreAuthorize("hasAuthority('user:device:list')")
    @PermissionName("获得任意账号登录设备列表")
    @GetMapping("/devices")
    public Result getDeviceList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer subId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String keyword
    ){


        return Result.success("获得登录设备列表成功" , loginDeviceService.getDeviceListBySubId(page, size, subId, deviceId, keyword));
    }

    @PreAuthorize("hasAuthority('user:device:status:me')")
    @PermissionName("修改本人登录设备状态")
    @PutMapping("/devices/status/me")
    public Result updateMyDeviceStatus(
            @RequestParam(required = true) String deviceId,
            @RequestParam(required = true) Integer status
    ){
        // 1正常 2停用(同时做下线处理)
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("状态参数不合法");
        }
        Integer subId = UserContext.getSubId();
        loginDeviceService.updateDeviceStatus(subId, deviceId, status);

        return Result.success("修改本人登录设备状态成功" , null);
    }

    @PreAuthorize("hasAuthority('user:device:status')")
    @PermissionName("修改任意账号登录设备状态")
    @PutMapping("/devices/status")
    public Result updateDeviceStatus(
            @RequestParam(required = true) Integer subId,
            @RequestParam(required = true) String deviceId,
            @RequestParam(required = true) Integer status
    ){
        // 1正常 2停用(同时做下线处理) 3禁用
        loginDeviceService.updateDeviceStatus(subId, deviceId, status);

        return Result.success("修改任意账号登录设备状态成功" , null);
    }

    @PreAuthorize("hasAuthority('user:device:delete:me')")
    @PermissionName("删除登录设备")
    @PutMapping("/devices/delete/me")
    public Result deleteMyDevice(
            @RequestParam(required = true) String deviceId
    ){
        // 判断设备是否属于该主账号
        Integer masterId = UserContext.getMasterId();
        if (!loginDeviceService.isMyDevice(deviceId, masterId)) {
            throw new BusinessException("无权操作该设备");
        }

        // 做下线处理,再删除设备
        loginDeviceService.deleteDevice(deviceId);

        return Result.success("删除成功" , null);
    }

    @PreAuthorize("hasAuthority('user:device:delete')")
    @PermissionName("删除任意登录设备")
    @PutMapping("/devices/delete")
    public Result deleteDevice(
            @RequestParam(required = true) String deviceId
    ){
        // 做下线处理,再删除设备
        loginDeviceService.deleteDevice(deviceId);

        return Result.success("删除成功" , null);
    }


    @PreAuthorize("hasAuthority('user:log:list:me')")
    @PermissionName("获得本人登录日志列表")
    @GetMapping("/logs/me")
    public Result getMyLogsList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String keyword
    ){

        Integer subId = UserContext.getSubId();

        return Result.success("获得本人登录设备列表成功" , loginDeviceService.getLogsList(page, size, subId, deviceId, keyword));
    }

    @PreAuthorize("hasAuthority('user:log:list')")
    @PermissionName("获得任意账号登录日志列表")
    @GetMapping("/logs")
    public Result getLogsList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer subId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String keyword
    ){


        return Result.success("获得登录设备列表成功" , loginDeviceService.getLogsList(page, size, subId, deviceId, keyword));
    }




}
