package com.alibaba.sdk.android.httpdns.interpret;

/**
 * 模式控制接口
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public interface StatusControl {
    /**
     * 下调
     */
    void turnDown();

    /**
     * 上调
     */
    void turnUp();
}
