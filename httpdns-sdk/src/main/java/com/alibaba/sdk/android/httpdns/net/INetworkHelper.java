package com.alibaba.sdk.android.httpdns.net;

public interface INetworkHelper {

    String generateCurrentNetworkId();

    boolean isWifi();

    boolean isMobile();
}
