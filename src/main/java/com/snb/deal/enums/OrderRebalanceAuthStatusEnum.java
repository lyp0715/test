package com.snb.deal.enums;

public enum OrderRebalanceAuthStatusEnum {

    PROCESS(1,"处理中"),
    COMPLETE(2,"授权完成"),
    END(3,"结束"),
    ABANDON(4,"作废")
    ;

    private int code;
    private String desc;

    OrderRebalanceAuthStatusEnum(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
