package com.example.user.security;

import com.example.common.auth.UserIdentity;
import com.example.common.auth.UserIdentityCodec;
import com.example.user.service.PermissionService;
import com.example.user.service.RoleService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户信息认证过滤器。
 *
 * <p>从网关透传的统一身份头中解析出 {@link UserIdentity}（头名与解析逻辑统一由
 * {@link UserIdentityCodec} 定义），再通过业务接口（而非直接访问数据库）查询权限点，
 * 最终构造带权限集合的 {@code Authentication} 放入安全上下文，供下游的 {@code @PreAuthorize} 授权校验使用。</p>
 */
@Component
@RequiredArgsConstructor
public class UserInfoAuthFilter extends OncePerRequestFilter {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 网关已校验 token，只要存在账号标识即视为已认证；
        // 即使没有角色，也保留一个空权限的 Authentication，让 @PreAuthorize 走 403 而不是 401。
        UserIdentity identity = UserIdentityCodec.read(request);
        if (identity.isPresent()) {
            List<GrantedAuthority> authorities = loadAuthorities(identity);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(identity.userId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 根据用户 ID 查询权限点（perm_key）作为授权标识。
     *
     * <p>角色与权限点的查询（含 RBAC 缓存）由 {@link RoleService} / {@link PermissionService}
     * 内部完成，本过滤器只负责组装 {@code Authentication}。</p>
     */
    private List<GrantedAuthority> loadAuthorities(UserIdentity identity) {
        Set<String> permKeys = new LinkedHashSet<>();
        for (Long roleId : roleService.getRoleIdsByUserId(identity.userId())) {
            permKeys.addAll(permissionService.getPermKeysByRoleId(roleId));
        }

        return permKeys.stream()
                .map(permKey -> (GrantedAuthority) new SimpleGrantedAuthority(permKey))
                .toList();
    }
}
