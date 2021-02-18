package com.alibaba.sdk.android.httpdns.request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * 网络请求失败的异常
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class HttpException extends Exception {
    public static final int DEGRADE_CODE = 403;
    public static final String DEGRADE_MESSAGE = "ServiceLevelDeny";
    public static final String ERROR_MSG_TAG = "code";
    private int code;

    private HttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Http status code
     *
     * @return
     */
    public int getCode() {
        return code;
    }

    /**
     * 创建异常
     *
     * @param code
     * @param message 服务器返回的特定格式的数据
     * @return
     */
    public static HttpException create(int code, String message) {
        return new HttpException(code, tryTranslateMessage(message));
    }

    private static String tryTranslateMessage(String message) {
        JSONObject object = null;
        try {
            object = new JSONObject(message);
            return object.getString(ERROR_MSG_TAG);
        } catch (JSONException ignored) {
        }
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpException that = (HttpException) o;
        return code == that.code && getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{code, getMessage()});
    }

    /**
     * 是不是服务降级了
     *
     * @return
     */
    public boolean isServerDegrade() {
        return code == DEGRADE_CODE && getMessage().equals(DEGRADE_MESSAGE);
    }
}
