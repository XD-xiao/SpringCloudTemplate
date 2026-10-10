package com.example.user.controller;

import com.example.common.domain.Entity.UserSystem.SysPermission;
import com.example.common.domain.Entity.UserSystem.SysRole;
import com.example.common.domain.Result;
import com.example.user.permission.PermissionName;
import com.example.user.service.PermissionService;
import com.example.user.service.RoleService;
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

/**
 * 系统管理控制器（角色与权限点）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/admin")
public class SystemAdminController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    // ==================== 角色管理 ====================

    @PreAuthorize("hasAuthority('admin:role:list')")
    @PermissionName("角色列表")
    @GetMapping("/roles")
    public Result getRoleList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取角色列表成功", roleService.getRolePage(current, pageSize, keyword));
    }

    @PreAuthorize("hasAuthority('admin:role:permlist')")
    @PermissionName("该角色权限列表")
    @GetMapping("/roles/permlist")
    public Result getPermListByRole(@RequestParam Long roleId) {
        return Result.success("获取权限列表成功", roleService.getPermissionIdsByRoleId(roleId));
    }

    @PreAuthorize("hasAuthority('admin:role:add')")
    @PermissionName("新增角色")
    @PostMapping("/roles/add")
    public Result createRole(
            @RequestParam(required = false) String roleName,
            @RequestParam String roleKey
    ) {
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        roleService.addRole(role);
        return Result.success("新增角色成功", null);
    }

    @PreAuthorize("hasAuthority('admin:role:update')")
    @PermissionName("修改角色")
    @PutMapping("/roles/update")
    public Result updateRole(
            @RequestParam Long roleId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey
    ) {
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        roleService.updateRole(role);
        return Result.success("修改角色成功", null);
    }

    @PreAuthorize("hasAuthority('admin:role:delete')")
    @PermissionName("删除角色")
    @DeleteMapping("/roles/delete")
    public Result deleteRole(@RequestParam Long roleId) {
        roleService.deleteRoleById(roleId);
        return Result.success("删除角色成功", null);
    }

    @PreAuthorize("hasAuthority('admin:role:permission')")
    @PermissionName("角色分配权限")
    @PutMapping("/roles/permission")
    public Result updateRolePermission(
            @RequestParam Long roleId,
            @RequestParam(required = false) List<Long> permissionIds
    ) {
        roleService.updateRolePermissions(roleId, permissionIds);
        return Result.success("分配权限成功", null);
    }

    // ==================== 用户-角色分配 ====================

    @PreAuthorize("hasAuthority('admin:role:list')")
    @PermissionName("全部角色")
    @GetMapping("/roles/all")
    public Result getAllRoles() {
        return Result.success("获取全部角色成功", roleService.getRoleList());
    }

    @PreAuthorize("hasAuthority('admin:user:role:list')")
    @PermissionName("用户已分配角色")
    @GetMapping("/user/roles")
    public Result getUserRoleIds(@RequestParam Long userId) {
        return Result.success("获取用户角色成功", roleService.getRoleIdsByUserId(userId));
    }

    @PreAuthorize("hasAuthority('admin:user:role:assign')")
    @PermissionName("分配用户角色")
    @PutMapping("/user/roles")
    public Result updateUserRoles(
            @RequestParam Long userId,
            @RequestParam(required = false) List<Long> roleIds
    ) {
        roleService.updateUserRoles(userId, roleIds);
        return Result.success("分配角色成功", null);
    }

    // ==================== 权限管理 ====================

    @PreAuthorize("hasAuthority('admin:perm:list')")
    @PermissionName("权限列表")
    @GetMapping("/permissions")
    public Result getPermissionList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword
    ) {
        long current = page == null ? 1 : page;
        long pageSize = size == null ? 10 : size;
        return Result.success("获取权限列表成功", permissionService.getPermissionPage(current, pageSize, keyword));
    }

    @PreAuthorize("hasAuthority('admin:perm:add')")
    @PermissionName("新增权限")
    @PostMapping("/permissions/add")
    public Result createPermission(
            @RequestParam String permName,
            @RequestParam String permKey,
            @RequestParam Integer permType,
            @RequestParam(required = false) String path
    ) {
        SysPermission permission = new SysPermission();
        permission.setPermName(permName);
        permission.setPermKey(permKey);
        permission.setPermType(permType);
        permission.setPath(path);
        permissionService.addPermission(permission);
        return Result.success("新增权限成功", null);
    }

    @PreAuthorize("hasAuthority('admin:perm:update')")
    @PermissionName("修改权限")
    @PutMapping("/permissions/update")
    public Result updatePermission(
            @RequestParam Long id,
            @RequestParam String permName,
            @RequestParam String permKey,
            @RequestParam Integer permType,
            @RequestParam(required = false) String path
    ) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermName(permName);
        permission.setPermKey(permKey);
        permission.setPermType(permType);
        permission.setPath(path);
        permissionService.updatePermission(permission);
        return Result.success("修改权限成功", null);
    }

    @PreAuthorize("hasAuthority('admin:perm:delete')")
    @PermissionName("删除权限")
    @DeleteMapping("/permissions/delete")
    public Result deletePermission(@RequestParam Long id) {
        permissionService.deletePermissionById(id);
        return Result.success("删除权限成功", null);
    }
}
