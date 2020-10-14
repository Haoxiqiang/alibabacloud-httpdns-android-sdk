package com.alibaba.sdk.android.httpdns.net64;

import android.text.TextUtils;

/**
 * Created by tomchen on 2018/9/14
 *
 * @author lianke
 */
public class Net64Utils {

    static public boolean isIPV6Address(String ip) {
        if (TextUtils.isEmpty(ip)) {
            return false;
        }
        char[] bytes = ip.toCharArray();

        if (bytes.length < 2) {
            return false;
        }

        int cntOfColon = 0;
        boolean isLastCharColon = true;
        boolean continuousColonAppear = false;

        int n = 0;

        int i = 0;
        if (bytes[i] == ':') {
            cntOfColon++;
            if (bytes[++i] != ':') //如果第一个字符是':'，第二个字符必须也是':'
                return false;
        }

        for (; i < bytes.length; i++) {
            char c = bytes[i];
            int cval = Character.digit(c, 16);
            if (cval != -1) {
                n = (n << 4) + cval;
                if (n > 0xffff) {
                    return false;
                }
                isLastCharColon = false;
            } else if (c == ':') {
                if (++cntOfColon > 7) {
                    return false;
                }

                if (isLastCharColon) {
                    if (continuousColonAppear) { //连续::只能出现一次
                        return false;
                    }
                    continuousColonAppear = true;
                    continue;
                }

                n = 0;
                isLastCharColon = true;
            } else {
                return false;
            }
        }

        if (!continuousColonAppear && cntOfColon < 7) {
            return false;
        }

        return true;
    }
}
