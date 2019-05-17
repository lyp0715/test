package com.snb.deal.enums;

public enum OrderRebalanceBuyModeEnum {

    FUNDMODELSCALE("0","原型比例"),
    INVESTMENTSCALE("1","现值比例"),
    ;

    private String code;
    private String desc;

    OrderRebalanceBuyModeEnum(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
