package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;

/**
 * 域名解析策略控制
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class CategoryController implements StatusControl {

    private Status status = Status.NORMAL;
    private NormalCategory normal;
    private SniffCategory sniff;

    public CategoryController(ScheduleService scheduleService) {
        normal = new NormalCategory(scheduleService, this);
        sniff = new SniffCategory(scheduleService, this);
    }

    public InterpretHostCategory getCategory() {
        switch (status) {
            case DISABLE:
                return sniff;
            default:
                return normal;
        }
    }

    @Override
    public void turnDown() {
        switch (status) {
            case NORMAL:
                status = Status.PRE_DISABLE;
                break;
            case PRE_DISABLE:
                status = Status.DISABLE;
                break;
            default:
                break;
        }
    }

    @Override
    public void turnUp() {
        status = Status.NORMAL;
    }

    /**
     * 重置策略
     */
    public void reset() {
        status = Status.NORMAL;
        sniff.reset();
    }

    /**
     * 设置嗅探模式请求间隔
     * @param timeInterval
     */
    public void setSniffTimeInterval(int timeInterval) {
        sniff.setInterval(timeInterval);
    }

    /**
     * 策略状态，只有disable状态会使用嗅探模式
     */
    enum Status {
        NORMAL,
        PRE_DISABLE,
        DISABLE
    }
}
