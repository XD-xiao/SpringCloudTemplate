package com.example.user.service;


import com.example.common.domain.PageBean;

public interface LoginDeviceService {


    PageBean getDeviceListBySubId(Integer page,Integer size,Integer subId,String deviceId,String keyword);

    void updateDeviceStatus(Integer subId,String deviceId,Integer status);

    void kickDevice(Integer masterId, Integer subId, String deviceId);

    void clearCookie();

    boolean isMyDevice(String deviceId , Integer masterId);

    void deleteDevice(String deviceId);


    PageBean getLogsList(Integer page,Integer size,Integer subId,String deviceId,String keyword);

}
