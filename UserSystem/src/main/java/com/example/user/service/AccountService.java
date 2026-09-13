package com.example.user.service;

import com.example.common.domain.PageBean;
import com.example.common.domain.vo.UserSystem.AuthCodeVO;
import com.example.common.domain.vo.UserSystem.MasterInfoVO;

import java.util.List;

public interface AccountService {

    AuthCodeVO createAuthorize(String email, String appKey);

    void logout(); //与设备下线类似

    Long createSub(String email,String code,String aliasName);

    PageBean getMasters(Integer page,Integer size,String keyword);

    MasterInfoVO getMasterInfo();

    void updateMasterStatus(Integer masterId,Integer status);

    PageBean getSubListByMasterId(Integer page,Integer size,String keyword,Integer masterId);

    void updateSub(Integer subId,String aliasName);

    void updateSubStatus(Integer subId,Integer status);

    // 判断马甲账号是否属于主账号
    boolean isMySub(Integer masterId,Integer subId);

    void kickSub(Integer subId);

    void updateSubRole(Integer subId, List<Long> roleIds);

    List<Long> getRoleListBySub(Long subId);

}
