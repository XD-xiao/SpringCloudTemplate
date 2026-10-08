package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.SysPermission;
import com.example.common.domain.Entity.UserSystem.SysRolePermission;
import com.example.common.domain.PageBean;
import com.example.user.cache.UserAuthorityCache;
import com.example.user.mapper.SysPermissionMapper;
import com.example.user.mapper.SysRolePermissionMapper;
import com.example.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * {@link PermissionService} 实现。
 *
 * <p>鉴权查询（{@link #getPermKeysByRoleIds}）与权限管理均落在本实现中；
 * 权限点本身或角色-权限关系变更后，会失效用户权限缓存，保证下次鉴权读取到最新数据。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    /** 权限类型：接口 */
    private static final int PERM_TYPE_API = 3;

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final UserAuthorityCache authorityCache;

    @Override
    public List<String> getPermKeysByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .in(SysRolePermission::getRoleId, roleIds)
                                .select(SysRolePermission::getPermId))
                .stream()
                .map(SysRolePermission::getPermId)
                .distinct()
                .toList();

        if (permIds.isEmpty()) {
            return List.of();
        }

        return permissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermission>()
                                .in(SysPermission::getId, permIds)
                                .select(SysPermission::getPermKey))
                .stream()
                .map(SysPermission::getPermKey)
                .distinct()
                .toList();
    }

    @Override
    public void upsertPermission(String permKey, String permName, String path) {
        String name = (permName == null || permName.isBlank()) ? permKey : permName;
        LocalDateTime now = LocalDateTime.now();

        SysPermission existing = getPermissionByPermKey(permKey);
        if (existing == null) {
            SysPermission permission = new SysPermission();
            permission.setPermKey(permKey);
            permission.setPermName(name);
            permission.setPermType(PERM_TYPE_API);
            permission.setPath(path);
            permission.setCreateTime(now);
            permission.setUpdateTime(now);
            permissionMapper.insert(permission);
        } else {
            existing.setPermName(name);
            existing.setPermType(PERM_TYPE_API);
            existing.setPath(path);
            existing.setUpdateTime(now);
            permissionMapper.updateById(existing);
        }
    }

    @Override
    public void deleteMissingPermissions(Set<String> scannedKeys) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermType, PERM_TYPE_API);
        if (scannedKeys != null && !scannedKeys.isEmpty()) {
            wrapper.notIn(SysPermission::getPermKey, scannedKeys);
        }

        List<SysPermission> removed = permissionMapper.selectList(wrapper);
        if (removed.isEmpty()) {
            return;
        }

        List<Long> ids = removed.stream().map(SysPermission::getId).toList();
        permissionMapper.deleteBatchIds(ids);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getPermId, ids));
        authorityCache.evictAll();
    }

    @Override
    public PageBean<SysPermission> getPermissionPage(long page, long size, String keyword) {
        Page<SysPermission> result = permissionMapper.selectPage(new Page<>(page, size), buildQuery(keyword));
        return new PageBean<>(result.getTotal(), result.getRecords());
    }

    @Override
    public List<SysPermission> getPermissionList() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId));
    }

    @Override
    public SysPermission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public SysPermission getPermissionByPermKey(String permKey) {
        return permissionMapper.selectOne(
                new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getPermKey, permKey));
    }

    @Override
    public void addPermission(SysPermission permission) {
        LocalDateTime now = LocalDateTime.now();
        permission.setCreateTime(now);
        permission.setUpdateTime(now);
        permissionMapper.insert(permission);
        authorityCache.evictAll();
    }

    @Override
    public void updatePermission(SysPermission permission) {
        permission.setUpdateTime(LocalDateTime.now());
        permissionMapper.updateById(permission);
        authorityCache.evictAll();
    }

    @Override
    public void deletePermissionById(Long id) {
        permissionMapper.deleteById(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermId, id));
        authorityCache.evictAll();
    }

    @Override
    public void deletePermissionByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        permissionMapper.deleteBatchIds(ids);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getPermId, ids));
        authorityCache.evictAll();
    }

    private LambdaQueryWrapper<SysPermission> buildQuery(String keyword) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysPermission::getPermName, keyword)
                    .or()
                    .like(SysPermission::getPermKey, keyword);
        }
        wrapper.orderByAsc(SysPermission::getId);
        return wrapper;
    }
}
