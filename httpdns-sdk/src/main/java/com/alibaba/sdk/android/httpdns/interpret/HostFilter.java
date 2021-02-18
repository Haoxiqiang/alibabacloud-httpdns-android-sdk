package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.DegradationFilter;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class HostFilter {
    DegradationFilter filter;

    public boolean isFiltered(String host) {
        return filter != null && filter.shouldDegradeHttpDNS(host);
    }

    public void setFilter(DegradationFilter filter) {
        this.filter = filter;
    }
}
