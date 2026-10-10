package com.example.user.permission;

import com.example.user.cache.UserAuthorityCache;
import com.example.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
 * <p>数据库读写统一委托给业务接口（{@link PermissionService}），
 * 本组件不再直接访问 Mapper。</p>
 *
 * <p>同步规则：</p>
 * <ul>
 *     <li>按 {@code perm_key} 幂等处理：不存在则插入，存在则更新 {@code path}；</li>
 *     <li>{@code perm_type} 固定为 3（接口）；</li>
 *     <li>{@code perm_name} 优先取 {@link PermissionName} 注解，未配置时回退为 {@code perm_key}；</li>
 *     <li>同步结束后清理类型为接口、但代码中已不存在的权限点。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionSyncRunner implements ApplicationRunner {

    /** 匹配 hasAuthority('xxx') */
    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    /** 匹配 hasAnyAuthority('a','b') */
    private static final Pattern HAS_ANY_AUTHORITY = Pattern.compile("hasAnyAuthority\\(([^)]*)\\)");

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PermissionService permissionService;
    private final UserAuthorityCache authorityCache;

    @Override
    public void run(ApplicationArguments args) {
        syncPermissions();
    }

    /**
     * 执行权限点扫描与同步。
     */
    public void syncPermissions() {
        // 先清空缓存，避免同步过程中残留旧的权限点缓存，再逐步写入最新数据
        authorityCache.evictAll();

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
                upsert(point.getKey(), point.getValue(), path);
            }
        }

        cleanupRemovedPermissions(scannedKeys);
        log.info("权限点同步完成，当前接口权限点数量：{}", scannedKeys.size());
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
     * 按 perm_key 幂等写入权限点。
     *
     * @param key   权限标识
     * @param name  权限名称，{@code null} 表示未配置 {@link PermissionName}
     * @param path  接口路径
     */
    private void upsert(String key, String name, String path) {
        permissionService.upsertPermission(key, name, path);
    }

    /**
     * 清理类型为接口、但代码中已不存在的权限点。
     */
    private void cleanupRemovedPermissions(Set<String> scannedKeys) {
        permissionService.deleteMissingPermissions(scannedKeys);
    }
}
