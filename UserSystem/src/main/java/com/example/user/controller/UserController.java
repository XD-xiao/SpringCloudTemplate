package com.example.user.controller;

import com.example.common.domain.Result;
import com.example.common.exception.BusinessException;
import com.example.common.utils.UserContext;
import com.example.user.permission.PermissionName;
import com.example.user.service.DeviceService;
import com.example.user.service.LoginLogService;
import com.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/user")
public class UserController {

    private final UserService userService;
    private final LoginLogService loginLogService;
    private final DeviceService deviceService;

    @PreAuthorize("hasAuthority('user:info:get')")
    @PermissionName("获得用户信息")
    @GetMapping("/userInfo")
    public Result getUserInfo(
            @RequestParam Long userId
    ) {
        return Result.success("获取用户信息成功", userService.getUserInfo(userId));
    }

    @PreAuthorize("hasAuthority('user:info:update:me')")
    @PermissionName("修改本人信息")
    @PutMapping("/userInfo/update/me")
    public Result updateMyUserInfo(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String birthday,
            @RequestParam(required = false) String bio
    ) {
        Long userId = UserContext.get().userId();
        LocalDate birthdayDate = null;
        if (birthday != null && !birthday.isBlank()) {
            try {
                birthdayDate = LocalDate.parse(birthday);
            } catch (DateTimeParseException e) {
                throw new BusinessException(400, "生日格式应为 yyyy-MM-dd");
            }
        }
        userService.updateMyUserInfo(userId, nickname, avatar, gender, birthdayDate, bio);
        return Result.success("修改本人信息成功", null);
    }

    @PreAuthorize("hasAuthority('user:status:update')")
    @PermissionName("修改用户状态信息")
    @PutMapping("/userStatus/update")
    public Result updateUserStatus(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status
    ) {
        userService.updateUserStatus(userId, status);
        return Result.success("修改用户状态信息成功", null);
    }

    @PreAuthorize("hasAuthority('user:info:list')")
    @PermissionName("获得用户列表")
    @GetMapping("/userList")
    public Result getUserList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer gender
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取用户列表成功", userService.getUserList(current, pageSize, email, nickname, gender));
    }

    @PreAuthorize("hasAuthority('user:login:log:me')")
    @PermissionName("获得本人登录日志")
    @GetMapping("/loginLog/me")
    public Result getMyLoginLog(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Long userId = UserContext.get().userId();
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取本人登录日志成功", loginLogService.getLoginLogPage(current, pageSize, userId));
    }

    @PreAuthorize("hasAuthority('user:login:log')")
    @PermissionName("获得任意用户登录日志")
    @GetMapping("/loginLog")
    public Result getLoginLog(
            @RequestParam Long userId,      //sys_user.id
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取任意用户登录日志成功", loginLogService.getLoginLogPage(current, pageSize, userId));
    }

    @PreAuthorize("hasAuthority('user:device:list:me')")
    @PermissionName("获得本人设备列表")
    @GetMapping("/deviceList/me")
    public Result getMyDeviceList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String fingerprint,
            @RequestParam(required = false) Integer status
    ) {
        Long userId = UserContext.get().userId();
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取本人设备列表成功", deviceService.getUserDevicePage(current, pageSize, userId, fingerprint, status));
    }

    @PreAuthorize("hasAuthority('user:device:update:me')")
    @PermissionName("修改本人设备状态")
    @PutMapping("/deviceList/update/me")
    public Result updateMyDeviceStatus(
            @RequestParam Long id,         //user_device.id
            @RequestParam(required = false) Integer status
    ) {
        Long userId = UserContext.get().userId();
        deviceService.updateUserDeviceStatusById(userId, id, status);
        return Result.success("修改本人设备状态成功", null);
    }

    @PreAuthorize("hasAuthority('user:device:list')")
    @PermissionName("获得用户设备列表")
    @GetMapping("/deviceList/user")
    public Result getUserDeviceList(
            @RequestParam Long userId,           //sys_user.id
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String fingerprint,
            @RequestParam(required = false) Integer status
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取用户设备列表成功", deviceService.getUserDevicePage(current, pageSize, userId, fingerprint, status));
    }
}
