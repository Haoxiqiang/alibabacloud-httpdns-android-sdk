package com.alibaba.sdk.android.httpdns.probe;


/**
 * Created by liyazhou on 2017/12/14.
 */

public interface IPProbeTaskCallback {
    void onIPProbeTaskFinished(long taskNumber, IPProbeOptimizeItem item);
}
