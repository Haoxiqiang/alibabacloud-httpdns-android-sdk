package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.request.ResponseTranslator;

/**
 * @author zonglin.nzl
 * @date 2020/12/9
 */
public class ResolveHostResponseTranslator implements ResponseTranslator<ResolveHostResponse> {
    @Override
    public ResolveHostResponse translate(String response) throws Throwable {
        return ResolveHostResponse.fromResponse(response);
    }
}
