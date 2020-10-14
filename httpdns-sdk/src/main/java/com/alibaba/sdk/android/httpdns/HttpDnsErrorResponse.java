package com.alibaba.sdk.android.httpdns;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liyazhou on 17/4/13.
 */

class HttpDnsErrorResponse {
    private int errorCode;
    private String errorMsg;
    private static final String ERROR_MSG_TAG = "code";

    HttpDnsErrorResponse(int code, String message) throws JSONException {
        this.errorCode = code;
        JSONObject object = new JSONObject(message);
        errorMsg = object.getString(ERROR_MSG_TAG);
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }
}
