package com.example.user.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限点名称注解，配合 {@code @PreAuthorize} 使用。
 *
 * <p>用于在权限点自动同步时，为 {@code sys_permission.perm_name} 提供可读名称。
 * 未配置时，同步组件会回退使用权限标识 {@code perm_key} 作为名称。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionName {

    /** 权限点名称 */
    String value();
}
