package com.example.common.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 身份契约的唯一来源。
 *
 * <p>统一维护「JWT claim 名」与「内部身份头名」的映射。网关解析 JWT、透传身份头、
 * 下游解析身份头、签发 JWT 都从这里取名字，业务调整字段时只改这一处即可。</p>
 *
 * <p>身份头白名单即 {@link #IDENTITY_HEADERS}，网关据此清除外部伪造身份头，
 * Feign 据此整批透传，二者语义一致，不会各自维护一份。</p>
 */
public final class AuthHeaders {

    /** 原始凭证头。 */
    public static final String AUTHORIZATION = "Authorization";

    /** JWT claim 名与内部身份头名的映射项。 */
    public record Field(String claim, String header) {
    }

    /** 马甲账号 id。 */
    public static final Field ACCOUNT_ID = new Field("account_id", "X-Account-Id");

    /** 设备 id。 */
    public static final Field DEVICE_ID = new Field("device_id", "X-Device-Id");

    /** 应用标识。 */
    public static final Field APP_KEY = new Field("app_key", "X-App-Key");

    /** 角色集合。 */
    public static final Field ROLES = new Field("roles", "X-Roles");

    /** 全部身份字段（有序，作为派生白名单与解析/签发的基准）。 */
    public static final List<Field> IDENTITY_FIELDS = List.of(
            ACCOUNT_ID, DEVICE_ID, APP_KEY, ROLES);

    /** 身份头白名单（由 {@link #IDENTITY_FIELDS} 派生）。 */
    public static final Set<String> IDENTITY_HEADERS = IDENTITY_FIELDS.stream()
            .map(Field::header)
            .collect(Collectors.toUnmodifiableSet());

    private AuthHeaders() {
    }

    /**
     * 判断请求头是否属于身份头（大小写不敏感，白名单匹配）。
     *
     * @param name 请求头名
     * @return true 表示命中 {@link #IDENTITY_HEADERS} 白名单
     */
    public static boolean isIdentityHeader(String name) {
        if (name == null) {
            return false;
        }
        return IDENTITY_HEADERS.stream().anyMatch(header -> header.equalsIgnoreCase(name));
    }
}
