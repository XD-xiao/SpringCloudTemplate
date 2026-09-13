package com.example.common.interceptors;

import com.example.common.utils.UserContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;

public class UserInfoInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {

        // 1.获取网关透传的用户信息头
        String accountId = request.getHeader("X-Account-Id");
        String masterId = request.getHeader("X-Master-Id");
        String deviceId = request.getHeader("X-Device-Id");
        String appKey = request.getHeader("X-App-Key");
        String rolesHeader = request.getHeader("X-Roles");
        // 2.判断主标识是否为空
        if (StrUtil.isNotBlank(accountId)) {
            Integer subId = Integer.valueOf(accountId);
            Integer masterIdVal = StrUtil.isNotBlank(masterId) ? Integer.valueOf(masterId) : null;
            List<String> roles = StrUtil.isNotBlank(rolesHeader)
                    ? Arrays.stream(rolesHeader.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                    : List.of();
            // 不为空，保存到ThreadLocal
            UserContext.setUser(subId, masterIdVal, deviceId, appKey, roles);
            // 同时存入 RequestAttributes，供 CircuitBreaker 跨线程传播
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute("userId", subId, RequestAttributes.SCOPE_REQUEST);
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
            attrs.removeAttribute("userId", RequestAttributes.SCOPE_REQUEST);
        }
    }
}
