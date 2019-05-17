package com.snb.deal.enums;

public enum OrderBusinessEnum {
    AUTO_INVEST(1,"定投"),
    MANUL_INVEST(2,"手动购买"),
    REDEEM(3,"赎回"),
    REBALANCE(4,"调仓"),
    DIVIDEND(5,"分红"),
    FORCE_REDEEM(6,"强制赎回"),
    ;

    private int code;
    private String desc;
    OrderBusinessEnum(int code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
