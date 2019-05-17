package com.snb.deal.enums;

public enum OrderRebalanceFailureBusiTypeEnum {


    SYNC_REBALANCE_SUMMARY(1,"同步调仓概要"),
    SEND_REBALANCE_ORDER(2,"发送调仓订单"),
    SYNC_REBALANCE_ORDER(3,"调仓订单同步"),
    ;

    private int code;
    private String desc;

    OrderRebalanceFailureBusiTypeEnum(Integer code, String desc){
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