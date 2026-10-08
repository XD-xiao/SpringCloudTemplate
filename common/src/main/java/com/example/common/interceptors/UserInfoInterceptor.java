package com.example.common.interceptors;

import com.example.common.auth.UserIdentity;
import com.example.common.auth.UserIdentityCodec;
import com.example.common.utils.UserContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserInfoInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {

        // 1.解析网关透传的统一身份头（头名与解析逻辑统一定义在 UserIdentityCodec）
        UserIdentity identity = UserIdentityCodec.read(request);
        // 2.存在账号标识则写入上下文
        if (identity.isPresent()) {
            UserContext.set(identity);
            // 同时存入 RequestAttributes，供 CircuitBreaker 跨线程传播
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute(UserIdentityCodec.REQUEST_ATTRIBUTE, identity, RequestAttributes.SCOPE_REQUEST);
        }
        // 3.放行
        return true;
    }

    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
        // 清理 RequestAttributes
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.removeAttribute(UserIdentityCodec.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
