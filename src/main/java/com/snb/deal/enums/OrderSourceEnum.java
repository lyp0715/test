package com.snb.deal.enums;

public enum OrderSourceEnum {
    H5("H5","H5"),
    ;

    private String code;
    private String desc;
    OrderSourceEnum(String code, String desc){
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
