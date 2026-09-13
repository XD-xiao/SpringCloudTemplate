package com.example.user.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.domain.Entity.UserSystem.ApplicationTable;
import com.example.common.domain.Entity.UserSystem.PermissionTable;
import com.example.common.domain.Entity.UserSystem.RolePermissionTable;
import com.example.common.domain.Entity.UserSystem.RoleTable;
import com.example.user.mapper.ApplicationMapper;
import com.example.user.mapper.PermissionMapper;
import com.example.user.mapper.RoleMapper;
import com.example.user.mapper.RolePermissionMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 用户信息认证过滤器。
 *
 * 从网关透传的 {@code X-Account-Id} / {@code X-App-Key} / {@code X-Roles} 请求头中
 * 解析用户标识与角色，再通过角色查询权限点，最终构造带权限集合的 {@code Authentication}
 * 放入安全上下文，供下游的 {@code @PreAuthorize} 授权校验使用。
 */
@Component
@RequiredArgsConstructor
public class UserInfoAuthFilter extends OncePerRequestFilter {

    /** 网关透传的马甲账号 id */
    private static final String HEADER_ACCOUNT_ID = "X-Account-Id";

    /** 网关透传的应用标识 */
    private static final String HEADER_APP_KEY = "X-App-Key";

    /** 网关透传的角色集合（逗号分隔的 role_key） */
    private static final String HEADER_ROLES = "X-Roles";

    private final ApplicationMapper applicationMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 网关已校验 token，只要存在账号标识即视为已认证；
        // 即使没有角色，也保留一个空权限的 Authentication，让 @PreAuthorize 走 403 而不是 401。
        String accountId = request.getHeader(HEADER_ACCOUNT_ID);
        if (accountId != null && !accountId.isBlank()) {
            List<GrantedAuthority> authorities = loadAuthorities(request);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(accountId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 根据网关透传的角色集合查询数据库，返回权限点（perm_key）作为授权标识。
     */
    private List<GrantedAuthority> loadAuthorities(HttpServletRequest request) {
        String appKey = request.getHeader(HEADER_APP_KEY);
        String rolesHeader = request.getHeader(HEADER_ROLES);
        if (appKey == null || appKey.isBlank() || rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }

        List<String> roleKeys = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .toList();
        if (roleKeys.isEmpty()) {
            return List.of();
        }

        ApplicationTable app = applicationMapper.selectOne(
                new QueryWrapper<ApplicationTable>().eq("app_key", appKey).eq("status", 1));
        if (app == null) {
            return List.of();
        }

        List<Long> roleIds = roleMapper.selectList(
                        new QueryWrapper<RoleTable>().in("role_key", roleKeys).eq("app_id", app.getId()))
                .stream().map(RoleTable::getId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new QueryWrapper<RolePermissionTable>().in("role_id", roleIds))
                .stream().map(RolePermissionTable::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return permissionMapper.selectList(
                        new QueryWrapper<PermissionTable>().in("id", permissionIds).eq("app_id", app.getId()))
                .stream()
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.getPermKey()))
                .toList();
    }
}
