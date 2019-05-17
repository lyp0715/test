package com.snb.deal.enums;

public enum OrderStatusEnum {
    PROCESS(1,"处理中"),
    COMPLETE(2,"完成"),
    APPLY_FAIL(3,"申请失败"),
    ABANDON(4,"作废")
    ;

    private int code;
    private String desc;
    OrderStatusEnum(int code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


    public static OrderStatusEnum getOrderStatus(int code) {
        OrderStatusEnum result = null;
        OrderStatusEnum[] orderStatusEnums = OrderStatusEnum.values();
        for (OrderStatusEnum orderStatusEnum : orderStatusEnums) {
            if (code == orderStatusEnum.getCode()) {
                result = orderStatusEnum;
                break;
            }
        }
        return result;
    }
}
