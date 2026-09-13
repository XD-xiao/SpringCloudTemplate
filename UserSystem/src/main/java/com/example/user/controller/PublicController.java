package com.example.user.controller;


import com.example.common.domain.Result;
import com.example.user.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/public")
public class PublicController {

    private final PublicService publicService;

    // 主账号注册
    @PostMapping("/master/register")
    public Result registerMaster(
            @RequestParam(required = true) String idCard,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String code
    ) {
        return Result.success("注册成功",publicService.registerMaster(idCard, email, code));
    }

    // 主账号登录
    @PostMapping("/master/login")
    public Result loginMaster(
            @RequestParam(required = true) String idCard,
            @RequestParam(required = true) List<String> mnemonic,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String deviceId,

            @RequestParam(required = false) String browser,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String location
    ) {
        return Result.success("登录成功",publicService.loginMaster(
                idCard,
                mnemonic,
                email,
                deviceId,
                browser,
                os,
                deviceType,
                userAgent,
                timeZone,
                location
        ));
    }

    // 马甲账号 邮箱登录
    @PostMapping("/sub/login/email")
    public Result loginSubByEmail(
            @RequestParam(required = true) String appKey,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String code,
            @RequestParam(required = true) String deviceId,

            @RequestParam(required = false) String browser,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String location
    ){


        return Result.success("登录成功",publicService.loginSubByEmail(
                appKey,
                email,
                code,
                deviceId,
                browser,
                os,
                deviceType,
                userAgent,
                timeZone,
                location
        ));
    }

    // 马甲账号 授权码登录
    @PostMapping("/sub/login/auth")
    public Result loginSubByAuth(
            @RequestParam(required = true) String appKey,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String code,
            @RequestParam(required = true) String deviceId,

            @RequestParam(required = false) String browser,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String location
    ){
        return Result.success("登录成功",publicService.loginSubByAuth(
                appKey,
                email,
                code,
                deviceId,
                browser,
                os,
                deviceType,
                userAgent,
                timeZone,
                location
        ));
    }


    // 发送验证码
    @PostMapping("/sendcode")
    public Result sendCode(
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String appKey,
            @RequestParam(required = true) String type
    ){

        String message = publicService.sendCode(email, appKey, type);
        return Result.success(message, null);
    }

    @PostMapping("/refresh")
    public Result refreshToken(
            @RequestParam(required = true) String deviceId
    ){


        return Result.success("" , publicService.refreshToken(deviceId));
    }

    @PostMapping("/import/permissions")
    public Result importPermissions(
            @RequestParam(required = true) MultipartFile file
    ) {
        publicService.importPermissions(file);
        return Result.success("导入成功", null);
    }



}