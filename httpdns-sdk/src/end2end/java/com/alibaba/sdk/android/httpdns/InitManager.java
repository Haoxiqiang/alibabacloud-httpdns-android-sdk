package com.alibaba.sdk.android.httpdns;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 1/14/22
 */
public class InitManager {

    private static class Holder {
        private static final InitManager instance = new InitManager();
    }

    public static InitManager getInstance() {
        return Holder.instance;
    }

    private InitManager() {
    }

    private HashMap<String, BeforeHttpDnsServiceInit> initThings = new HashMap<>();


    public void add(String accountId, BeforeHttpDnsServiceInit beforeHttpDnsServiceInit) {
        initThings.put(accountId, beforeHttpDnsServiceInit);
    }

    public BeforeHttpDnsServiceInit getAndRemove(String accountId) {
        return initThings.remove(accountId);
    }
}
