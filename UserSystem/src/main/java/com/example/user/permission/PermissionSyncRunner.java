package com.example.user.permission;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.domain.Entity.UserSystem.ApplicationTable;
import com.example.common.domain.Entity.UserSystem.PermissionTable;
import com.example.user.mapper.ApplicationMapper;
import com.example.user.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 权限点自动同步组件。
 *
 * <p>应用启动完成后自动扫描所有已注册的 HandlerMethod，
 * 读取方法（或类）上的 {@link PreAuthorize} 注解，提取其中的
 * {@code hasAuthority('...')} / {@code hasAnyAuthority('...')} 权限标识，
 * 并增量同步到 {@code sys_permission} 表。</p>
 *
 * <p>同步规则：</p>
 * <ul>
 *     <li>以当前应用的 {@code app_key} 解析 {@code app_id}，找不到则跳过并记录日志；</li>
 *     <li>按 {@code (app_id, perm_key)} 幂等处理：不存在则插入，存在则更新 {@code path}；</li>
 *     <li>{@code perm_type} 固定为 3（接口）；{@code parent_id} 仅首次添加时默认为 0，已存在则不修改；</li>
 *     <li>{@code perm_name} 优先取 {@link PermissionName} 注解，未配置时回退为 {@code perm_key}；</li>
 *     <li>同步结束后清理当前应用下、类型为接口、但代码中已不存在的权限点。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionSyncRunner implements ApplicationRunner {

    /** 接口类型权限点 */
    private static final int PERM_TYPE_INTERFACE = 3;

    /** 根权限父 id */
    private static final long PARENT_ID_ROOT = 0L;

    /** 匹配 hasAuthority('xxx') */
    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    /** 匹配 hasAnyAuthority('a','b') */
    private static final Pattern HAS_ANY_AUTHORITY = Pattern.compile("hasAnyAuthority\\(([^)]*)\\)");

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PermissionMapper permissionMapper;
    private final ApplicationMapper applicationMapper;

    @Value("${user-system.app-key:UserSystem}")
    private String appKey;

    @Override
    public void run(ApplicationArguments args) {
        syncPermissions();
    }

    /**
     * 执行权限点扫描与同步。
     */
    public void syncPermissions() {
        Long appId = resolveAppId();
        if (appId == null) {
            log.warn("未找到 app_key={} 对应的 sys_application 记录，跳过权限点同步", appKey);
            return;
        }

        Set<String> scannedKeys = new LinkedHashSet<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            Map<String, String> points = extractPermissionPoints(entry.getValue());
            if (points.isEmpty()) {
                continue;
            }
            String path = buildPath(entry.getKey());
            for (Map.Entry<String, String> point : points.entrySet()) {
                scannedKeys.add(point.getKey());
                upsert(appId, point.getKey(), point.getValue(), path);
            }
        }

        cleanupRemovedPermissions(appId, scannedKeys);
        log.info("权限点同步完成，当前应用接口权限点数量：{}", scannedKeys.size());
    }

    /**
     * 根据配置的 app_key 查询 app_id。
     */
    private Long resolveAppId() {
        ApplicationTable app = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey));
        return app == null ? null : app.getId();
    }

    /**
     * 从 HandlerMethod 的方法或所属类上提取权限点（权限标识 -> 权限名称）。
     * 名称优先取 {@link PermissionName}，未配置时值为 {@code null}。
     */
    private Map<String, String> extractPermissionPoints(HandlerMethod handlerMethod) {
        Set<String> keys = extractPermissionKeys(handlerMethod);
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        String name = resolvePermissionName(handlerMethod);
        Map<String, String> points = new LinkedHashMap<>();
        for (String key : keys) {
            // 名称仅对单一权限标识有意义；多个权限标识无法一一对应，回退为 null
            points.put(key, keys.size() == 1 ? name : null);
        }
        return points;
    }

    /**
     * 从 HandlerMethod 的方法或所属类上提取 {@link PreAuthorize} 中的权限标识。
     */
    private Set<String> extractPermissionKeys(HandlerMethod handlerMethod) {
        PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), PreAuthorize.class);
        if (preAuthorize == null) {
            preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), PreAuthorize.class);
        }
        if (preAuthorize == null) {
            return Collections.emptySet();
        }
        return parseAuthorities(preAuthorize.value());
    }

    /**
     * 提取方法或类上的 {@link PermissionName}，未配置返回 {@code null}。
     */
    private String resolvePermissionName(HandlerMethod handlerMethod) {
        PermissionName permissionName = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), PermissionName.class);
        if (permissionName == null) {
            permissionName = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), PermissionName.class);
        }
        return permissionName == null ? null : permissionName.value();
    }

    /**
     * 解析 SpEL 表达式中的权限标识。
     */
    private Set<String> parseAuthorities(String expression) {
        Set<String> keys = new LinkedHashSet<>();

        Matcher authorityMatcher = HAS_AUTHORITY.matcher(expression);
        while (authorityMatcher.find()) {
            keys.add(authorityMatcher.group(1).trim());
        }

        Matcher anyAuthorityMatcher = HAS_ANY_AUTHORITY.matcher(expression);
        while (anyAuthorityMatcher.find()) {
            for (String part : anyAuthorityMatcher.group(1).split(",")) {
                String key = part.trim();
                if (key.length() >= 2 && key.startsWith("'") && key.endsWith("'")) {
                    key = key.substring(1, key.length() - 1);
                }
                if (!key.isBlank()) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    /**
     * 组合 HTTP 方法与接口路径，如 {@code POST /api/userservice/subs/create}。
     */
    private String buildPath(RequestMappingInfo info) {
        Set<String> patterns = resolvePatterns(info);
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

        String methodPart = methods.isEmpty()
                ? "ANY"
                : methods.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        String pathPart = patterns.stream().sorted().collect(Collectors.joining(","));

        return methodPart + " " + pathPart;
    }

    private Set<String> resolvePatterns(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        if (info.getPatternsCondition() != null) {
            return info.getPatternsCondition().getPatterns();
        }
        return Collections.emptySet();
    }

    /**
     * 按 (app_id, perm_key) 幂等写入权限点。
     *
     * @param name 权限名称，{@code null} 表示未配置 {@link PermissionName}
     */
    private void upsert(Long appId, String key, String name, String path) {
        PermissionTable existing = permissionMapper.selectOne(
                new QueryWrapper<PermissionTable>()
                        .eq("app_id", appId)
                        .eq("perm_key", key));

        if (existing == null) {
            PermissionTable permission = new PermissionTable();
            permission.setAppId(appId);
            permission.setPermName(name == null ? key : name);
            permission.setPermKey(key);
            permission.setPermType(PERM_TYPE_INTERFACE);
            permission.setParentId(PARENT_ID_ROOT);
            permission.setPath(path);
            permission.setCreateTime(LocalDateTime.now());
            permission.setUpdateTime(LocalDateTime.now());
            permissionMapper.insert(permission);
            log.info("新增权限点：{} -> {}", key, path);
            return;
        }

        boolean changed = false;
        String targetName = name != null
                ? name
                : (existing.getPermName() == null || existing.getPermName().isBlank() ? key : null);
        if (targetName != null && !targetName.equals(existing.getPermName())) {
            existing.setPermName(targetName);
            changed = true;
        }
        if (!Objects.equals(existing.getPath(), path)) {
            existing.setPath(path);
            changed = true;
        }
        if (changed) {
            existing.setUpdateTime(LocalDateTime.now());
            permissionMapper.updateById(existing);
            log.info("更新权限点：{} -> {}", key, path);
        }
    }

    /**
     * 清理当前应用下、类型为接口、但代码中已不存在的权限点。
     */
    private void cleanupRemovedPermissions(Long appId, Set<String> scannedKeys) {
        List<PermissionTable> existingList = permissionMapper.selectList(
                new QueryWrapper<PermissionTable>()
                        .eq("app_id", appId)
                        .eq("perm_type", PERM_TYPE_INTERFACE));

        for (PermissionTable permission : existingList) {
            if (!scannedKeys.contains(permission.getPermKey())) {
                permissionMapper.deleteById(permission.getId());
                log.info("删除已不存在的权限点：{}", permission.getPermKey());
            }
        }
    }
}
