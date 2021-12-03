package com.aliyun.ams.httpdns.demo.utils;

public interface INetworkHelper {

    String generateCurrentNetworkId();

    boolean isWifi();

    boolean isMobile();
}
