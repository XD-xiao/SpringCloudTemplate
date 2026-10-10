package com.example.user.service;

import com.example.common.domain.Entity.UserSystem.SysRole;
import com.example.common.domain.PageBean;

import java.util.List;

/**
 * 角色域业务接口。
 *
 * <p>覆盖三类使用场景：</p>
 * <ul>
 *     <li>鉴权：按用户 ID 查询角色 ID 集合，供安全过滤器后续解析权限；</li>
 *     <li>角色管理：角色（sys_role）的增删改查；</li>
 *     <li>关系分配：角色-权限、用户-角色的授权维护。</li>
 * </ul>
 */
public interface RoleService {

    // ==================== 鉴权 ====================

    /**
     * 按用户 ID 查询角色 ID 集合（走 RBAC 缓存，未命中查库回填）。
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 按用户 ID 查询角色标识（role_key）集合（走 RBAC 缓存，供登录/刷新签发用）。
     */
    List<String> getRoleKeysByUserId(Long userId);

    // ==================== 角色管理 ====================

    /**
     * 分页查询角色。
     */
    PageBean<SysRole> getRolePage(long page, long size, String keyword);

    /**
     * 查询全部角色（供用户授权下拉使用）。
     */
    List<SysRole> getRoleList();

    /**
     * 按 ID 查询角色。
     */
    SysRole getRoleById(Long id);

    /**
     * 按 role_key 查询角色（唯一性校验用）。
     */
    SysRole getRoleByRoleKey(String roleKey);

    /**
     * 新增角色。
     */
    void addRole(SysRole role);

    /**
     * 修改角色。
     */
    void updateRole(SysRole role);

    /**
     * 按 ID 删除角色。
     */
    void deleteRoleById(Long id);

    /**
     * 按 ID 集合批量删除角色。
     */
    void deleteRoleByIds(List<Long> ids);

    // ==================== 角色-权限分配 ====================

    /**
     * 按角色 ID 查询已分配的权限 ID 集合。
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 覆盖式分配角色权限（先删后插）。
     */
    void updateRolePermissions(Long roleId, List<Long> permIds);

    // ==================== 用户-角色分配 ====================

    /**
     * 覆盖式分配用户角色（先删后插）。
     */
    void updateUserRoles(Long userId, List<Long> roleIds);
}
