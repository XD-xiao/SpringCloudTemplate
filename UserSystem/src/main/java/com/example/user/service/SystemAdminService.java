package com.example.user.service;

import com.example.common.domain.PageBean;

import java.util.List;

public interface SystemAdminService {

    PageBean getAppList(Integer page,Integer size,String keyword);

    void createApp(String appName,String appKey,Integer status);

    void updateApp(Long id,String appName,String appKey,Integer status);

    void updateAppStatus(Long id,Integer status);

    PageBean getRoleList(Integer page,Integer size,String keyword,String appKey);

    List<Long> getPermListByRole(Long roleId);

    void createRole(String roleName,String roleKey);

    void updateRole(Long roleId,String roleName,String roleKey);

    void deleteRole(Long roleId);

    void updateRolePermission(Long roleId, List<Long> permissionIds);

    PageBean getPermissionList(Integer page,Integer size,String keyword, String appKey);

    void createPermission(String permName,String permKey,Integer permType,String path);

    void updatePermission(Long id,String permName,String permKey,Integer permType,String path);

    void deletePermission(Long id);

    PageBean getMnemonicWordList(Integer page,Integer size,String keyword);

    void createMnemonicWord(String word);

    void updateMnemonicWordStatus(Long id,Integer status);

    void deleteMnemonicWord(Long id);


}
