package com.snb.deal.enums;

/**
 * 保险状态
 */
public enum InsuranceStatusEnum {
    UNCLAIMED(1,"未领取"),
    REVIEWING(2,"审核中"),
    GUARANTEED(3,"保障中"),
    ;

    private int code;
    private String desc;
    InsuranceStatusEnum(int code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


    public static InsuranceStatusEnum getInsuranceStatus(int code) {
        InsuranceStatusEnum result = null;
        InsuranceStatusEnum[] insuranceStatusEnums = InsuranceStatusEnum.values();
        for (InsuranceStatusEnum insuranceStatusEnum : insuranceStatusEnums) {
            if (code == insuranceStatusEnum.getCode()) {
                result = insuranceStatusEnum;
                break;
            }
        }
        return result;
    }
}
