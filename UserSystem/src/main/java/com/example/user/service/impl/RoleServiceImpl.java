package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.Entity.UserSystem.SysPermission;
import com.example.common.domain.Entity.UserSystem.SysRole;
import com.example.common.domain.Entity.UserSystem.SysRolePermission;
import com.example.common.domain.Entity.UserSystem.SysUserRole;
import com.example.common.domain.PageBean;
import com.example.common.exception.BusinessException;
import com.example.user.cache.UserAuthorityCache;
import com.example.user.mapper.SysPermissionMapper;
import com.example.user.mapper.SysRoleMapper;
import com.example.user.mapper.SysRolePermissionMapper;
import com.example.user.mapper.SysUserRoleMapper;
import com.example.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link RoleService} 实现。
 *
 * <p>角色、用户-角色、角色-权限三类数据的维护均在此实现；
 * 授权关系或角色本身变更后，会失效相关用户的权限缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final UserAuthorityCache authorityCache;

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Long> roleIds = authorityCache.getRoleIds(userId);
        if (roleIds != null) {
            return roleIds;
        }
        roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, userId)
                                .select(SysUserRole::getRoleId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
        authorityCache.putRoleIds(userId, roleIds);
        return roleIds;
    }

    @Override
    public List<String> getRoleKeysByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .in(SysRole::getId, roleIds)
                                .select(SysRole::getRoleKey))
                .stream()
                .map(SysRole::getRoleKey)
                .filter(roleKey -> roleKey != null && !roleKey.isBlank())
                .toList();
    }

    @Override
    public PageBean<SysRole> getRolePage(long page, long size, String keyword) {
        Page<SysRole> result = roleMapper.selectPage(new Page<>(page, size), buildQuery(keyword));
        return new PageBean<>(result.getTotal(), result.getRecords());
    }

    @Override
    public List<SysRole> getRoleList() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Override
    public SysRole getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public SysRole getRoleByRoleKey(String roleKey) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey));
    }

    @Override
    public void addRole(SysRole role) {
        LocalDateTime now = LocalDateTime.now();
        role.setCreateTime(now);
        role.setUpdateTime(now);
        roleMapper.insert(role);
    }

    @Override
    public void updateRole(SysRole role) {
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        authorityCache.evictAll();
    }

    @Override
    public void deleteRoleById(Long id) {
        roleMapper.deleteById(id);
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        authorityCache.evictAll();
    }

    @Override
    public void deleteRoleByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        roleMapper.deleteBatchIds(ids);
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, ids));
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, ids));
        authorityCache.evictAll();
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .eq(SysRolePermission::getRoleId, roleId)
                                .select(SysRolePermission::getPermId))
                .stream()
                .map(SysRolePermission::getPermId)
                .toList();
    }

    @Override
    public void updateRolePermissions(Long roleId, List<Long> permIds) {
        List<Long> normalizedPermIds = normalizeIds(permIds);
        if (!normalizedPermIds.isEmpty()) {
            Long count = permissionMapper.selectCount(
                    new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, normalizedPermIds));
            if (count == null || count != normalizedPermIds.size()) {
                throw new BusinessException(400, "存在不存在的权限 ID，请刷新后重试");
            }
        }

        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        for (Long permId : normalizedPermIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermId(permId);
            rolePermissionMapper.insert(relation);
        }
        authorityCache.evictRole(roleId);
    }

    @Override
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeIds(roleIds);
        if (!normalizedRoleIds.isEmpty()) {
            Long count = roleMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, normalizedRoleIds));
            if (count == null || count != normalizedRoleIds.size()) {
                throw new BusinessException(400, "存在不存在的角色 ID，请刷新后重试");
            }
        }

        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : normalizedRoleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
        authorityCache.evictUser(userId);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private LambdaQueryWrapper<SysRole> buildQuery(String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRoleKey, keyword);
        }
        wrapper.orderByAsc(SysRole::getId);
        return wrapper;
    }
}
