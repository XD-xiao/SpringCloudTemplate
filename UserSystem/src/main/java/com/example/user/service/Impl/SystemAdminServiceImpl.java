package com.example.user.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.domain.PageBean;
import com.example.common.domain.Entity.UserSystem.ApplicationTable;
import com.example.common.domain.Entity.UserSystem.MnemonicWordTable;
import com.example.common.domain.Entity.UserSystem.PermissionTable;
import com.example.common.domain.Entity.UserSystem.RolePermissionTable;
import com.example.common.domain.Entity.UserSystem.RoleTable;
import com.example.common.domain.Entity.UserSystem.SubAccountRoleTable;
import com.example.common.domain.vo.UserSystem.PermissionVO;
import com.example.common.domain.vo.UserSystem.RoleVO;
import com.example.common.exception.BusinessException;
import com.example.common.utils.UserContext;
import com.example.user.mapper.ApplicationMapper;
import com.example.user.mapper.MnemonicWordMapper;
import com.example.user.mapper.PermissionMapper;
import com.example.user.mapper.RoleMapper;
import com.example.user.mapper.RolePermissionMapper;
import com.example.user.mapper.SubAccountRoleMapper;
import com.example.user.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemAdminServiceImpl implements SystemAdminService {

    /** 应用标识（app_key）兜底值 */
    @Value("${user-system.app-key:UserSystem}")
    private String appKeyUser;

    /** 权限点缓存 key 前缀 */
    @Value("${user-system.permissions-cache.redis-prefix:auth:permission:}")
    private String permissionsCachePrefix;

    private final ApplicationMapper applicationMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final MnemonicWordMapper mnemonicWordMapper;
    private final SubAccountRoleMapper subAccountRoleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageBean<ApplicationTable> getAppList(Integer page, Integer size, String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<ApplicationTable> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("app_name", keyword).or().like("app_key", keyword));
        }
        wrapper.orderByDesc("id");

        Page<ApplicationTable> result = applicationMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return new PageBean<>(result.getTotal(), result.getRecords());
    }

    @Override
    public void createApp(String appName, String appKey, Integer status) {
        Long count = applicationMapper.selectCount(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey));
        if (count != null && count > 0) {
            throw new BusinessException("系统标识已存在");
        }

        ApplicationTable app = new ApplicationTable();
        app.setAppName(appName);
        app.setAppKey(appKey);
        app.setStatus(status == null ? 1 : status);
        applicationMapper.insert(app);
    }

    @Override
    public void updateApp(Long id, String appName, String appKey, Integer status) {
        ApplicationTable app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException("系统不存在");
        }

        if (appKey != null && !appKey.isBlank() && !appKey.equals(app.getAppKey())) {
            Long count = applicationMapper.selectCount(
                    new QueryWrapper<ApplicationTable>().eq("app_key", appKey).ne("id", id));
            if (count != null && count > 0) {
                throw new BusinessException("系统标识已存在");
            }
            app.setAppKey(appKey);
        }
        if (appName != null) {
            app.setAppName(appName);
        }
        if (status != null) {
            app.setStatus(status);
        }
        applicationMapper.updateById(app);
    }

    @Override
    public void updateAppStatus(Long id, Integer status) {
        ApplicationTable app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException("系统不存在");
        }
        app.setStatus(status);
        applicationMapper.updateById(app);
    }

    @Override
    public PageBean<RoleVO> getRoleList(Integer page, Integer size, String keyword, String appKey) {
        Long appId = resolveAppIdByKey(appKey);
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<RoleTable> wrapper = new QueryWrapper<>();
        wrapper.eq("app_id", appId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("role_name", keyword).or().like("role_key", keyword));
        }
        wrapper.orderByDesc("id");

        Page<RoleTable> result = roleMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<RoleVO> list = result.getRecords().stream().map(this::toRoleVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    public List<Long> getPermListByRole(Long roleId) {
        RoleTable role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        return rolePermissionMapper.selectList(
                        new QueryWrapper<RolePermissionTable>().eq("role_id", roleId))
                .stream().map(RolePermissionTable::getPermissionId).toList();
    }

    @Override
    public void createRole(String roleName, String roleKey) {
        Long appId = resolveAppIdByKey(currentAppKey());

        Long count = roleMapper.selectCount(
                new QueryWrapper<RoleTable>().eq("app_id", appId).eq("role_key", roleKey));
        if (count != null && count > 0) {
            throw new BusinessException("角色标识已存在");
        }

        RoleTable role = new RoleTable();
        role.setAppId(appId);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        roleMapper.insert(role);
    }

    @Override
    public void updateRole(Long roleId, String roleName, String roleKey) {
        RoleTable role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        if (roleKey != null && !roleKey.isBlank() && !roleKey.equals(role.getRoleKey())) {
            Long count = roleMapper.selectCount(
                    new QueryWrapper<RoleTable>()
                            .eq("app_id", role.getAppId())
                            .eq("role_key", roleKey)
                            .ne("id", roleId));
            if (count != null && count > 0) {
                throw new BusinessException("角色标识已存在");
            }
            role.setRoleKey(roleKey);
        }
        if (roleName != null) {
            role.setRoleName(roleName);
        }
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        RoleTable role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 删除角色与权限、马甲账号的关联，再删除角色本身
        rolePermissionMapper.delete(new QueryWrapper<RolePermissionTable>().eq("role_id", roleId));
        subAccountRoleMapper.delete(new QueryWrapper<SubAccountRoleTable>().eq("role_id", roleId));
        roleMapper.deleteById(roleId);

        // 清除该角色的权限点缓存
        evictPermissionCache(role);
    }

    @Override
    @Transactional
    public void updateRolePermission(Long roleId, List<Long> permissionIds) {
        RoleTable role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 去重，避免重复权限 id 影响数量校验
        List<Long> distinctIds = permissionIds == null || permissionIds.isEmpty()
                ? List.of()
                : permissionIds.stream().distinct().toList();

        // 校验权限均属于该角色所属系统，避免跨系统越权分配
        if (!distinctIds.isEmpty()) {
            Long count = permissionMapper.selectCount(
                    new QueryWrapper<PermissionTable>()
                            .in("id", distinctIds)
                            .eq("app_id", role.getAppId()));
            if (count == null || count != distinctIds.size()) {
                throw new BusinessException("存在不属于该系统的权限");
            }
        }

        // 覆盖写入角色权限关联
        rolePermissionMapper.delete(new QueryWrapper<RolePermissionTable>().eq("role_id", roleId));
        for (Long permissionId : distinctIds) {
            RolePermissionTable relation = new RolePermissionTable();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }

        // 清除该角色的权限点缓存
        evictPermissionCache(role);
    }

    @Override
    public PageBean<PermissionVO> getPermissionList(Integer page, Integer size, String keyword, String appKey) {
        Long appId = resolveAppIdByKey(appKey);
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<PermissionTable> wrapper = new QueryWrapper<>();
        wrapper.eq("app_id", appId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("perm_name", keyword)
                    .or().like("perm_key", keyword)
                    .or().like("path", keyword));
        }
        wrapper.orderByDesc("id");

        Page<PermissionTable> result = permissionMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<PermissionVO> list = result.getRecords().stream().map(this::toPermissionVO).toList();
        return new PageBean<>(result.getTotal(), list);
    }

    @Override
    public void createPermission(String permName, String permKey, Integer permType, String path) {
        Long appId = resolveAppIdByKey(currentAppKey());

        Long count = permissionMapper.selectCount(
                new QueryWrapper<PermissionTable>().eq("app_id", appId).eq("perm_key", permKey));
        if (count != null && count > 0) {
            throw new BusinessException("权限标识已存在");
        }

        PermissionTable permission = new PermissionTable();
        permission.setAppId(appId);
        permission.setPermName(permName);
        permission.setPermKey(permKey);
        permission.setPermType(permType);
        permission.setPath(path);
        permission.setParentId(0L);
        permissionMapper.insert(permission);
    }

    @Override
    public void updatePermission(Long id, String permName, String permKey, Integer permType, String path) {
        PermissionTable permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        if (permKey != null && !permKey.isBlank() && !permKey.equals(permission.getPermKey())) {
            Long count = permissionMapper.selectCount(
                    new QueryWrapper<PermissionTable>()
                            .eq("app_id", permission.getAppId())
                            .eq("perm_key", permKey)
                            .ne("id", id));
            if (count != null && count > 0) {
                throw new BusinessException("权限标识已存在");
            }
            permission.setPermKey(permKey);
        }
        if (permName != null) {
            permission.setPermName(permName);
        }
        if (permType != null) {
            permission.setPermType(permType);
        }
        if (path != null) {
            permission.setPath(path);
        }
        permissionMapper.updateById(permission);

        // 清除引用该权限的所有角色缓存
        evictPermissionCacheByPermissionId(id);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        PermissionTable permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        // 删除前先清除引用该权限的所有角色缓存
        evictPermissionCacheByPermissionId(id);

        rolePermissionMapper.delete(new QueryWrapper<RolePermissionTable>().eq("permission_id", id));
        permissionMapper.deleteById(id);
    }

    @Override
    public PageBean<MnemonicWordTable> getMnemonicWordList(Integer page, Integer size, String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;

        QueryWrapper<MnemonicWordTable> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("word", keyword);
        }
        wrapper.orderByDesc("id");

        Page<MnemonicWordTable> result = mnemonicWordMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return new PageBean<>(result.getTotal(), result.getRecords());
    }

    @Override
    public void createMnemonicWord(String word) {
        Long count = mnemonicWordMapper.selectCount(
                new QueryWrapper<MnemonicWordTable>().eq("word", word));
        if (count != null && count > 0) {
            throw new BusinessException("词条已存在");
        }

        MnemonicWordTable mnemonicWord = new MnemonicWordTable();
        mnemonicWord.setWord(word);
        mnemonicWord.setStatus(1);
        mnemonicWordMapper.insert(mnemonicWord);
    }

    @Override
    public void updateMnemonicWordStatus(Long id, Integer status) {
        MnemonicWordTable mnemonicWord = mnemonicWordMapper.selectById(id);
        if (mnemonicWord == null) {
            throw new BusinessException("词条不存在");
        }
        mnemonicWord.setStatus(status);
        mnemonicWordMapper.updateById(mnemonicWord);
    }

    @Override
    public void deleteMnemonicWord(Long id) {
        MnemonicWordTable mnemonicWord = mnemonicWordMapper.selectById(id);
        if (mnemonicWord == null) {
            throw new BusinessException("词条不存在");
        }
        mnemonicWordMapper.deleteById(id);
    }

    // ==================== 辅助方法 ====================

    private String currentAppKey() {
        String appKey = UserContext.getAppKey();
        return appKey == null || appKey.isBlank() ? appKeyUser : appKey;
    }

    private Long resolveAppIdByKey(String appKey) {
        ApplicationTable app = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey));
        if (app == null) {
            throw new BusinessException("系统不存在");
        }
        return app.getId();
    }

    private void evictPermissionCache(RoleTable role) {
        if (role == null) {
            return;
        }
        stringRedisTemplate.delete(permissionsCachePrefix + role.getAppId() + ":" + role.getRoleKey());
    }

    private void evictPermissionCacheByPermissionId(Long permissionId) {
        List<Long> roleIds = rolePermissionMapper.selectList(
                        new QueryWrapper<RolePermissionTable>().eq("permission_id", permissionId))
                .stream().map(RolePermissionTable::getRoleId).distinct().toList();
        for (Long roleId : roleIds) {
            RoleTable role = roleMapper.selectById(roleId);
            evictPermissionCache(role);
        }
    }

    private RoleVO toRoleVO(RoleTable role) {
        return new RoleVO(role.getId(), role.getRoleName(), role.getRoleKey());
    }

    private PermissionVO toPermissionVO(PermissionTable permission) {
        return new PermissionVO(
                permission.getId(),
                permission.getPermName(),
                permission.getPermKey(),
                permission.getPermType(),
                permission.getPath());
    }
}
