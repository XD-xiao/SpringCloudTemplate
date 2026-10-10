package com.example.user.controller;


import cn.hutool.extra.servlet.JakartaServletUtil;
import com.example.common.domain.Result;
import com.example.common.domain.TokenPair;
import com.example.common.exception.BusinessException;
import com.example.common.exception.UnauthorizedException;
import com.example.user.config.JwtProperties;
import com.example.user.service.AuthResult;
import com.example.user.service.PermissionService;
import com.example.user.service.PublicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/public")
public class PublicController {

    /** 长 Token Cookie 名称（RefreshToken-{appKey}）。 */
    private static final String REFRESH_COOKIE = "RefreshToken-UserSystem";

    private final PublicService publicService;
    private final PermissionService permissionService;
    private final JwtProperties jwtProperties;

    @GetMapping("getCode")
    public Result getCode(
            @RequestParam String email,
            @RequestParam Integer type                  // 1注册 2登录
    ){
        publicService.getCode(email, type);
        return Result.success("发送成功", null);
    }


    @PostMapping("/register")
    public Result register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String code
    ) {
        publicService.register(email, password, code);
        return Result.success("注册成功", null);
    }

    @PostMapping("/loginByPassword")
    public Result<TokenPair> loginByPassword(
            @RequestParam String email,
            @RequestParam String password,
            // 设备信息
            @RequestParam String fingerprint,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceCategory,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) String osVersion,
            @RequestParam(required = false) String browser,
            @RequestParam(required = false) String browserVersion,
            @RequestParam(required = false) String userAgent,
            // 登录信息
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String loginIp = JakartaServletUtil.getClientIP(request);
        AuthResult result = publicService.loginByPassword(email, password, fingerprint, deviceType, deviceCategory,
                osName, osVersion, browser, browserVersion, userAgent,
                loginIp, timezone, latitude, longitude);
        setRefreshCookie(response, result.refreshToken());
        return Result.success("登录成功", toTokenPair(result));
    }

    @PostMapping("/loginByCode")
    public Result<TokenPair> loginByCode(
            @RequestParam String email,
            @RequestParam String code,
            // 设备信息
            @RequestParam String fingerprint,
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
            @RequestParam(required = false) BigDecimal longitude,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String loginIp = JakartaServletUtil.getClientIP(request);
        AuthResult result = publicService.loginByCode(email, code, fingerprint, deviceType, deviceCategory,
                osName, osVersion, browserName, browserVersion, userAgent,
                loginIp, timezone, latitude, longitude);
        setRefreshCookie(response, result.refreshToken());
        return Result.success("登录成功", toTokenPair(result));
    }

    @PostMapping("/refresh")
    public Result<TokenPair> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ){
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("缺少 refresh token");
        }
        AuthResult result = publicService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return Result.success("", toTokenPair(result));
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ){
        publicService.logout(refreshToken);
        clearRefreshCookie(response);
        return Result.success("已退出登录", null);
    }

    @PostMapping("/syncPermissions")
    public Result<Void> syncPermissions(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        String json;
        try {
            json = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(400, "读取文件失败");
        }
        permissionService.syncFrontendPermissions(json);
        return Result.success("权限同步成功", null);
    }

    private TokenPair toTokenPair(AuthResult result) {
        return new TokenPair(result.accessToken(), result.expiresIn(), result.permissions());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/userservice/public")
                .maxAge(jwtProperties.getRefreshTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/userservice/public")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
