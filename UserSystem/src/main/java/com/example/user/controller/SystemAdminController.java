package com.example.user.controller;


import com.example.common.domain.Result;
import com.example.user.permission.PermissionName;
import com.example.user.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice")
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    // ==================== 系统管理 ====================

    @PreAuthorize("hasAuthority('user:app:list')")
    @PermissionName("系统列表")
    @GetMapping("/apps")
    public Result getAppList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success("获取系统列表成功", systemAdminService.getAppList(page, size, keyword));
    }

    @PreAuthorize("hasAuthority('user:app:add')")
    @PermissionName("新增系统")
    @PostMapping("/apps/add")
    public Result createApp(
            @RequestParam(required = true) String appName,
            @RequestParam(required = true) String appKey,
            @RequestParam(required = false) Integer status
    ) {
        systemAdminService.createApp(appName, appKey, status);
        return Result.success("新增系统成功", null);
    }

    @PreAuthorize("hasAuthority('user:app:update')")
    @PermissionName("修改系统")
    @PutMapping("/apps/update")
    public Result updateApp(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String appName,
            @RequestParam(required = true) String appKey,
            @RequestParam(required = false) Integer status
    ) {
        systemAdminService.updateApp(id, appName, appKey, status);
        return Result.success("修改系统成功", null);
    }

    @PreAuthorize("hasAuthority('user:app:status')")
    @PermissionName("停用/启用系统")
    @PutMapping("/apps/status")
    public Result updateAppStatus(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) Integer status
    ) {
        // 1启用 0停用
        systemAdminService.updateAppStatus(id, status);
        return Result.success("修改系统状态成功", null);
    }

    // ==================== 角色管理 ====================

    @PreAuthorize("hasAuthority('user:role:list')")
    @PermissionName("角色列表")
    @GetMapping("/roles")
    public Result getRoleList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = true) String appKey
    ) {
        return Result.success("获取角色列表成功", systemAdminService.getRoleList(page, size, keyword, appKey));
    }


    @PreAuthorize("hasAuthority('user:role:permlist')")
    @PermissionName("该角色权限列表")
    @GetMapping("/roles/permlist")
    public Result getPermListByRole(

            @RequestParam(required = true) Long roleId
    ) {
        return Result.success("获取权限列表成功", systemAdminService.getPermListByRole(roleId));
    }


    @PreAuthorize("hasAuthority('user:role:add')")
    @PermissionName("新增角色")
    @PostMapping("/roles/add")
    public Result createRole(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = true) String roleKey
    ) {
        systemAdminService.createRole(roleName, roleKey);
        return Result.success("新增角色成功", null);
    }

    @PreAuthorize("hasAuthority('user:role:update')")
    @PermissionName("修改角色")
    @PutMapping("/roles/update")
    public Result updateRole(
            @RequestParam(required = true) Long roleId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey
    ) {
        systemAdminService.updateRole(roleId, roleName, roleKey);
        return Result.success("修改角色成功", null);
    }

    @PreAuthorize("hasAuthority('user:role:delete')")
    @PermissionName("删除角色")
    @DeleteMapping("/roles/delete")
    public Result deleteRole(
            @RequestParam(required = true) Long roleId
    ) {
        systemAdminService.deleteRole(roleId);
        return Result.success("删除角色成功", null);
    }

    @PreAuthorize("hasAuthority('user:role:permission')")
    @PermissionName("角色分配权限")
    @PutMapping("/roles/permission")
    public Result updateRolePermission(
            @RequestParam(required = true) Long roleId,
            @RequestParam(required = true) List<Long> permissionIds
    ) {
        systemAdminService.updateRolePermission(roleId, permissionIds);
        return Result.success("分配权限成功", null);
    }

    // ==================== 权限管理 ====================

    @PreAuthorize("hasAuthority('user:perm:list')")
    @PermissionName("权限列表")
    @GetMapping("/permissions")
    public Result getPermissionList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = true) String appKey
    ) {
        return Result.success("获取权限列表成功", systemAdminService.getPermissionList(page, size, keyword, appKey));
    }

    @PreAuthorize("hasAuthority('user:perm:add')")
    @PermissionName("新增权限")
    @PostMapping("/permissions/add")
    public Result createPermission(
            @RequestParam(required = true) String permName,
            @RequestParam(required = true) String permKey,
            @RequestParam(required = true) Integer permType,
            @RequestParam(required = false) String path
    ) {
        systemAdminService.createPermission(permName, permKey, permType, path);
        return Result.success("新增权限成功", null);
    }

    @PreAuthorize("hasAuthority('user:perm:update')")
    @PermissionName("修改权限")
    @PutMapping("/permissions/update")
    public Result updatePermission(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String permName,
            @RequestParam(required = true) String permKey,
            @RequestParam(required = true) Integer permType,
            @RequestParam(required = false) String path
    ) {
        systemAdminService.updatePermission(id, permName, permKey, permType, path);
        return Result.success("修改权限成功", null);
    }

    @PreAuthorize("hasAuthority('user:perm:delete')")
    @PermissionName("删除权限")
    @DeleteMapping("/permissions/delete")
    public Result deletePermission(
            @RequestParam(required = true) Long id
    ) {
        systemAdminService.deletePermission(id);
        return Result.success("删除权限成功", null);
    }

    // ==================== 助记词词库管理 ====================

    @PreAuthorize("hasAuthority('user:mnemonic:list')")
    @PermissionName("词库列表")
    @GetMapping("/mnemonicwords")
    public Result getMnemonicWordList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success("获取词库列表成功", systemAdminService.getMnemonicWordList(page, size, keyword));
    }

    @PreAuthorize("hasAuthority('user:mnemonic:add')")
    @PermissionName("新增词条")
    @PostMapping("/mnemonicwords")
    public Result createMnemonicWord(
            @RequestParam(required = true) String word
    ) {
        systemAdminService.createMnemonicWord(word);
        return Result.success("新增词条成功", null);
    }

    @PreAuthorize("hasAuthority('user:mnemonic:update')")
    @PermissionName("停用/启用词条")
    @PutMapping("/mnemonicwords/update")
    public Result updateMnemonicWordStatus(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) Integer status
    ) {
        // 1启用 0停用
        systemAdminService.updateMnemonicWordStatus(id, status);
        return Result.success("修改词条状态成功", null);
    }

    @PreAuthorize("hasAuthority('user:mnemonic:delete')")
    @PermissionName("删除词条")
    @DeleteMapping("/mnemonicwords/delete")
    public Result deleteMnemonicWord(
            @RequestParam(required = true) Long id
    ) {
        systemAdminService.deleteMnemonicWord(id);
        return Result.success("删除词条成功", null);
    }

}
