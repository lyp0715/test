package com.snb.deal.enums;

import com.snb.common.enums.FundChannelEnum;
import org.apache.commons.lang.StringUtils;

public enum OrderRebalanceTransactionTypeEnum {

    ORDERREBALANCE_REDEEM(1,"sell","调仓赎回"),
    ORDERREBALANCE_INVEST(2,"buy","调仓申购"),
    ;

    private int code;
    private String desc;
    private String yfCode;

    OrderRebalanceTransactionTypeEnum(int code, String yfCode, String desc){
        this.code = code;
        this.desc = desc;
        this.yfCode = yfCode;
    }



    public static OrderRebalanceTransactionTypeEnum getRebalanceTransactionTypeEnum(FundChannelEnum fundChannelEnum, String yfCode) {
        OrderRebalanceTransactionTypeEnum result = null;
        if (StringUtils.isEmpty(yfCode)) {
            return result;
        }
        if (fundChannelEnum == FundChannelEnum.YIFENG) {
            OrderRebalanceTransactionTypeEnum[] rebalanceTransactionTypeEnums = OrderRebalanceTransactionTypeEnum.values();
            for (OrderRebalanceTransactionTypeEnum rebalanceTransactionTypeEnum : rebalanceTransactionTypeEnums) {
                if (StringUtils.isEmpty(rebalanceTransactionTypeEnum.getYfCode())) {
                    continue;
                }
                if (StringUtils.equals(yfCode, rebalanceTransactionTypeEnum.getYfCode())) {
                    result = rebalanceTransactionTypeEnum;
                    break;
                }
            }
        }
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public String getYfCode() {
        return yfCode;
    }

    public String getDesc() {
        return desc;
    }
}
