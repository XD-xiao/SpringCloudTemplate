package com.example.user.utils;

import com.example.common.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendCode {

    private final EmailUtil emailUtil;

    public void sendCode(String email, String code, String type) {
        String subject;
        String action;
        if ("login".equalsIgnoreCase(type)) {
            subject = "【StaticWorld】登录验证码";
            action = "登录";
        } else {
            subject = "【StaticWorld】注册验证码";
            action = "注册";
        }

        String content = "您的" + action + "验证码为：" + code + "，有效期50秒，请勿泄露给他人。";
        emailUtil.sendSimpleMail(email, subject, content);
    }
}
