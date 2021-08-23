package com.alibaba.sdk.android.httpdns.net;

import java.net.Inet6Address;

public class Nat64Prefix {
    public int mPrefixLength;

    public Inet6Address mPrefix;

    public Nat64Prefix(Inet6Address mPrefix, int mPrefixLength) {
        this.mPrefixLength = mPrefixLength;
        this.mPrefix = mPrefix;
    }

    @Override
    public String toString() {
        return mPrefix.getHostAddress() + "/" + mPrefixLength;
    }
}
