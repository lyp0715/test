package com.snb.deal.enums;

public enum OrderDetailStatusEnum {
    COMMIT_APPLY_SUCCESS(1,"提交申请成功"),
    COMMIT_APPLY_FAIL(2,"提交申请失败"),
    PRICE(3,"确认中"),
    COMPLETE(4,"完成"),
    FAIL(5,"失败"),
    PRICED(6,"确认份额")
    ;

    private int code;
    private String desc;
    OrderDetailStatusEnum(int code, String desc){
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
