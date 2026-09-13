package com.example.user.service;

import com.example.common.domain.vo.UserSystem.LoginResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PublicService {


    List<String> registerMaster(String idCard,String email,String code);

    LoginResultVO loginMaster(
            String idCard,
            List<String> mnemonic,
            String email,
            String deviceId,
            String browser,
            String os,
            String deviceTyp,
            String userAgent,
            String timeZone,
            String location

    );

    LoginResultVO loginSubByEmail(
            String appKey,
            String email,
            String code,
            String deviceId,
            String browser,
            String os,
            String deviceTyp,
            String userAgent,
            String timeZone,
            String location
    );

    LoginResultVO loginSubByAuth(
            String appKey,
            String email,
            String code,
            String deviceId,
            String browser,
            String os,
            String deviceTyp,
            String userAgent,
            String timeZone,
            String location
    );


    String sendCode(String email,String appKey,String type);

    LoginResultVO refreshToken(String deviceId);

    void importPermissions(MultipartFile file);

}
