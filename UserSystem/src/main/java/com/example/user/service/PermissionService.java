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
     * 按角色 ID 查询权限标识（perm_key）集合。
     *
     * <p>未指定类型时返回全部类型并走 RBAC 缓存；指定类型时按需过滤（直接查库，
     * 供登录/刷新取类型 1 页面 / 2 组件的权限点）。</p>
     *
     * @param roleId    角色 ID
     * @param permTypes 权限类型（1 页面 / 2 组件 / 3 接口）；为空表示全部类型
     * @return perm_key 集合
     */
    List<String> getPermKeysByRoleId(Long roleId, Integer... permTypes);

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

    /**
     * 解析前端权限 JSON 并同步页面 / 组件权限点到 sys_permission。
     *
     * <p>与 PermissionSyncRunner 同步接口权限点（perm_type = 3）相对，
     * 本方法同步 perm_type = 1（页面）/ 2（组件）的权限点：按 perm_key 幂等新增/更新，
     * 并清理类型为 1/2 且不在 JSON 内的失效权限点。</p>
     *
     * @param json permissions.json 的原始内容
     */
    void syncFrontendPermissions(String json);

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
