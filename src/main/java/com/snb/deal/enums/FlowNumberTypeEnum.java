package com.snb.deal.enums;

/**
 * Created by Thinkpad on 2018/4/14.
 */
public enum FlowNumberTypeEnum {

    YIFENG(1, "yf_flow_", "奕丰"),;

    private int flowType;
    private String key;
    private String desc;

    FlowNumberTypeEnum(int flowType, String key, String desc) {
        this.flowType = flowType;
        this.key = key;
        this.desc = desc;
    }

    public int getFlowType() {
        return flowType;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }
}
