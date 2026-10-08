package com.example.user.controller;


import com.example.common.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/public")
public class PublicController {

    @GetMapping("getCode")
    public Result getCode(
            @RequestParam String email,
            @RequestParam Integer type                  // 1注册 2登录
    ){

        return Result.success("", null);
    }


    @PostMapping("/register")
    public Result register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String code
    ) {

        return Result.success("", null);
    }

    @PostMapping("/loginByPassword")
    public Result loginByPassword(
            @RequestParam String email,
            @RequestParam String password,
            // 设备信息
            @RequestParam(required = false) String fingerprint,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceCategory,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) String osVersion,
            @RequestParam(required = false) String browserName,
            @RequestParam(required = false) String browserVersion,
            @RequestParam(required = false) String userAgent,
            // 登录信息
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude
    ) {

        return Result.success("", null);
    }

    @PostMapping("/loginByCode")
    public Result loginByCode(
            @RequestParam String email,
            @RequestParam String code,
            // 设备信息
            @RequestParam(required = false) String fingerprint,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceCategory,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) String osVersion,
            @RequestParam(required = false) String browserName,
            @RequestParam(required = false) String browserVersion,
            @RequestParam(required = false) String userAgent,
            // 登录信息
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude
    ) {

        return Result.success("", null);
    }

    @PostMapping("/refresh")
    public Result refresh(){

        return Result.success("", null);
    }


}
