package com.example.user.service;

import com.example.common.domain.Entity.UserSystem.SysPermission;
import com.example.common.domain.PageBean;

import java.util.List;
import java.util.Set;

/**
 * 权限域业务接口。
 *
 * <p>覆盖三类使用场景：</p>
 * <ul>
 *     <li>鉴权：按角色查询权限标识（perm_key）集合，供安全过滤器组装授权；</li>
 *     <li>权限扫描：启动时按 perm_key 幂等同步接口权限点，并清理失效权限点；</li>
 *     <li>权限管理：权限点（sys_permission）的增删改查。</li>
 * </ul>
 */
public interface PermissionService {

    // ==================== 鉴权 ====================

    /**
     * 按角色 ID 集合查询权限标识（perm_key）集合。
     *
     * @param roleIds 角色 ID 集合
     * @return perm_key 集合
     */
    List<String> getPermKeysByRoleIds(List<Long> roleIds);

    // ==================== 权限扫描 ====================

    /**
     * 按 perm_key 幂等新增/更新接口权限点（perm_type 固定为 3）。
     *
     * @param permKey  权限标识
     * @param permName 权限名称，为 {@code null} 时回退为 permKey
     * @param path     接口路径
     */
    void upsertPermission(String permKey, String permName, String path);

    /**
     * 删除接口类型（perm_type = 3）且不在给定集合内的失效权限点。
     *
     * @param scannedKeys 本次扫描得到的有效 perm_key 集合
     */
    void deleteMissingPermissions(Set<String> scannedKeys);

    // ==================== 权限管理 ====================

    /**
     * 分页查询权限点。
     */
    PageBean<SysPermission> getPermissionPage(long page, long size, String keyword);

    /**
     * 查询全部权限点（供角色授权勾选使用）。
     */
    List<SysPermission> getPermissionList();

    /**
     * 按 ID 查询权限点。
     */
    SysPermission getPermissionById(Long id);

    /**
     * 按 perm_key 查询权限点（唯一性校验用）。
     */
    SysPermission getPermissionByPermKey(String permKey);

    /**
     * 新增权限点。
     */
    void addPermission(SysPermission permission);

    /**
     * 修改权限点。
     */
    void updatePermission(SysPermission permission);

    /**
     * 按 ID 删除权限点。
     */
    void deletePermissionById(Long id);

    /**
     * 按 ID 集合批量删除权限点。
     */
    void deletePermissionByIds(List<Long> ids);
}
