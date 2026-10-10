package com.example.user.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.SysPermission;
import com.example.common.domain.Entity.UserSystem.SysRolePermission;
import com.example.common.domain.PageBean;
import com.example.common.exception.BusinessException;
import com.example.user.cache.UserAuthorityCache;
import com.example.user.mapper.SysPermissionMapper;
import com.example.user.mapper.SysRolePermissionMapper;
import com.example.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link PermissionService} 实现。
 *
 * <p>鉴权查询（{@link #getPermKeysByRoleId}）与权限管理均落在本实现中；
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
    public List<String> getPermKeysByRoleId(Long roleId, Integer... permTypes) {
        if (roleId == null) {
            return List.of();
        }

        // 未指定类型：走缓存（缓存角色全部权限点，鉴权高频）
        if (permTypes == null || permTypes.length == 0) {
            List<String> cached = authorityCache.getPermKeys(roleId);
            if (cached != null) {
                return cached;
            }
            List<String> keys = queryPermKeys(roleId, null);
            authorityCache.putPermKeys(roleId, keys);
            return keys;
        }

        // 指定类型：按需查库（登录/刷新低频，取类型 1 页面 / 2 组件）
        return queryPermKeys(roleId, Arrays.asList(permTypes));
    }

    private List<String> queryPermKeys(Long roleId, Collection<Integer> permTypes) {
        List<Long> permIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .eq(SysRolePermission::getRoleId, roleId)
                                .select(SysRolePermission::getPermId))
                .stream()
                .map(SysRolePermission::getPermId)
                .distinct()
                .toList();

        if (permIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permIds)
                .select(SysPermission::getPermKey);
        if (permTypes != null && !permTypes.isEmpty()) {
            wrapper.in(SysPermission::getPermType, permTypes);
        }

        return permissionMapper.selectList(wrapper)
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
    @Transactional(rollbackFor = Exception.class)
    public void syncFrontendPermissions(String json) {
        List<SysPermission> incoming = parsePermissionJson(json);
        Set<String> scannedKeys = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (SysPermission item : incoming) {
            String key = item.getPermKey();
            if (key == null || key.isBlank()) {
                throw new BusinessException(400, "权限标识 permKey 不能为空");
            }
            Integer type = item.getPermType();
            if (type == null || (type != 1 && type != 2)) {
                throw new BusinessException(400, "权限类型仅支持 1 页面 / 2 组件：" + key);
            }
            scannedKeys.add(key);
            upsertFrontPermission(item, now);
        }

        cleanupMissingFrontPermissions(scannedKeys);
        authorityCache.evictAll();
    }

    private List<SysPermission> parsePermissionJson(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(400, "上传内容不能为空");
        }
        try {
            return JSONUtil.toList(json, SysPermission.class);
        } catch (Exception e) {
            throw new BusinessException(400, "JSON 解析失败：" + e.getMessage());
        }
    }

    private void upsertFrontPermission(SysPermission item, LocalDateTime now) {
        String name = (item.getPermName() == null || item.getPermName().isBlank())
                ? item.getPermKey() : item.getPermName();

        SysPermission existing = getPermissionByPermKey(item.getPermKey());
        if (existing == null) {
            SysPermission permission = new SysPermission();
            permission.setPermKey(item.getPermKey());
            permission.setPermName(name);
            permission.setPermType(item.getPermType());
            permission.setPath(item.getPath());
            permission.setCreateTime(now);
            permission.setUpdateTime(now);
            permissionMapper.insert(permission);
        } else {
            existing.setPermName(name);
            existing.setPermType(item.getPermType());
            if (item.getPath() != null) {
                existing.setPath(item.getPath());
            }
            existing.setUpdateTime(now);
            permissionMapper.updateById(existing);
        }
    }

    private void cleanupMissingFrontPermissions(Set<String> scannedKeys) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getPermType, 1, 2);
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
