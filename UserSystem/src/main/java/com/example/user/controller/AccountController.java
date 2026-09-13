package com.example.user.controller;


import com.example.common.domain.Result;
import com.example.common.exception.BusinessException;
import com.example.common.utils.UserContext;
import com.example.user.permission.PermissionName;
import com.example.user.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice")
public class AccountController {

    private final AccountService accountService;

    @PreAuthorize("hasAuthority('user:sub:authorize')")
    @PermissionName("生成授权码")
    @PostMapping("/sub/authorize")
    public Result createAuthorize(
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String appKey
    ){

        return Result.success("生成授权码成功" , accountService.createAuthorize(email,appKey) );
    }

    @PreAuthorize("hasAuthority('user:sub:logout')")
    @PermissionName("登出")
    @PostMapping("/sub/logout")
    public Result logout(){

        accountService.logout();

        return Result.success("登出成功" , null );
    }




    @PreAuthorize("hasAuthority('user:sub:create')")
    @PermissionName("创建马甲账号")
    @PostMapping("/sub/create")
    public Result createSub(
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String code,
            @RequestParam(required = false) String aliasName
    ){

        return Result.success("创建马甲账号成功" , accountService.createSub(email, code, aliasName) );
    }

    @PreAuthorize("hasAuthority('user:master:list')")
    @PermissionName("获得主账号列表")
    @PostMapping("/masters")
    public Result getMasters(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ){


        return Result.success("获取主账号列表成功" , accountService.getMasters(page, size, keyword));
    }

    @PreAuthorize("hasAuthority('user:master:info')")
    @PermissionName("获得主账号信息")
    @PostMapping("/master/info")
    public Result getMasterInfo(){


        return Result.success("获取主账号信息成功" , accountService.getMasterInfo());
    }

    @PreAuthorize("hasAuthority('user:master:status')")
    @PermissionName("修改任意主账号状态")
    @PutMapping("/master/status")
    public Result updateMasterStatus(
            @RequestParam(required = true) Integer masterId,
            @RequestParam(required = true) Integer status
    ){
        // 1正常 2禁用
        accountService.updateMasterStatus(masterId, status);

        return Result.success("修改成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:list:me')")
    @PermissionName("获得本人马甲账号列表")
    @GetMapping("/subs/me")
    public Result getMySubList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ){

        Integer masterId = UserContext.getMasterId();

        return Result.success("获取马甲账号列表成功" , accountService.getSubListByMasterId(page, size, keyword, masterId));
    }

    @PreAuthorize("hasAuthority('user:sub:list')")
    @PermissionName("根据主账号获得任意马甲账号列表")
    @GetMapping("/subs")
    public Result getSubList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer masterId
    ){


        return Result.success("获取马甲账号列表成功" , accountService.getSubListByMasterId(page, size, keyword, masterId));
    }

    @PreAuthorize("hasAuthority('user:sub:rolelist')")
    @PermissionName("该用户角色列表")
    @GetMapping("/sub/rolelist")
    public Result getRoleListBySub(

            @RequestParam(required = true) Long subId
    ) {
        return Result.success("获取角色列表成功", accountService.getRoleListBySub(subId));
    }


    @PreAuthorize("hasAuthority('user:sub:update:me')")
    @PermissionName("修改本人马甲账号信息")
    @PutMapping("/sub/update/me")
    public Result updateMySub(
            @RequestParam(required = true) String aliasName
    ){
        Integer subId = UserContext.getSubId();
        accountService.updateSub(subId, aliasName);

        return Result.success("修改成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:update')")
    @PermissionName("修改任意马甲账号信息")
    @PutMapping("/sub/update")
    public Result updateSub(
            @RequestParam(required = true) Integer subId,
            @RequestParam(required = true) String aliasName
    ){

        accountService.updateSub(subId, aliasName);

        return Result.success("修改成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:status:me')")
    @PermissionName("修改本人马甲账号状态")
    @PutMapping("/sub/status/me")
    public Result updateMySubStatus(
            @RequestParam(required = true) Integer subId,
            @RequestParam(required = true) Integer status
    ){

        Integer masterId = UserContext.getMasterId();
        if (!accountService.isMySub(masterId, subId)) {
            throw new BusinessException("无权操作该马甲账号");
        }
        accountService.updateSubStatus(subId, status);

        return Result.success("修改成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:status')")
    @PermissionName("修改任意马甲账号状态")
    @PutMapping("/sub/status")
    public Result updateSubStatus(
            @RequestParam(required = true) Integer subId,
            @RequestParam(required = true) Integer status
    ){

        accountService.updateSubStatus(subId, status);

        return Result.success("修改成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:kick:me')")
    @PermissionName("下线本人马甲账号")
    @PostMapping("/sub/kick/me")
    public Result kickMySub(
            @RequestParam(required = true) Integer subId
    ){

        Integer masterId = UserContext.getMasterId();
        if (!accountService.isMySub(masterId, subId)) {
            throw new BusinessException("无权操作该马甲账号");
        }
        accountService.kickSub(subId);

        return Result.success("下线成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:kick')")
    @PermissionName("下线任意马甲账号")
    @PostMapping("/sub/kick")
    public Result kickSub(
            @RequestParam(required = true) Integer subId
    ){

        accountService.kickSub(subId);

        return Result.success("下线成功" , null);
    }

    @PreAuthorize("hasAuthority('user:sub:role:update')")
    @PermissionName("给任意马甲账号分配角色")
    @PutMapping("/sub/role/update")
    public Result updateSubRole(
            @RequestParam(required = true) Integer subId,
            @RequestParam(required = true) List<Long> roleIds
    ){

        accountService.updateSubRole(subId, roleIds);

        return Result.success("分配成功" , null);
    }




}
