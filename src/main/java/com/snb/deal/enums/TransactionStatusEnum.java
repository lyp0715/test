package com.snb.deal.enums;

import com.snb.common.enums.FundChannelEnum;
import org.apache.commons.lang.StringUtils;

/**
 * @Description 
 * @author lizengqiang
 * @date 2018/4/13 14:24
 */
public enum TransactionStatusEnum {
    SUCCESS(101, OrderStatusEnum.COMPLETE, "completed", "交易成功"),
    FAIL(102, OrderStatusEnum.COMPLETE, "void", "交易失败"),
    SUCCESS_PART(103, OrderStatusEnum.COMPLETE, "", "交易部分成功"),
    RECEIVING(200, OrderStatusEnum.PROCESS, "", "下单确认中"),
    RECEIVED(201, OrderStatusEnum.PROCESS, "received", "下单成功"),
    CANCELING(202, OrderStatusEnum.PROCESS, "canceling", "撤单中"),
    CANCELED(203, OrderStatusEnum.COMPLETE, "canceled", "已撤单"),
    PRICED(301, OrderStatusEnum.PROCESS, "priced", "确认成功"),
    IPO_PRICED(302, OrderStatusEnum.PROCESS, "ipo.processing", "认购确认成功"),
    WAIT_PAY(401, OrderStatusEnum.PROCESS, "pending.payment", "等待付款"),
    WAIT_REFUND(402, OrderStatusEnum.PROCESS, "pending.void", "等待退款"),
    PAYING(501, OrderStatusEnum.PROCESS, "payment", "支付过程中"),
    PAY_FAIL(502, OrderStatusEnum.COMPLETE, "failure", "支付失败"),
    ;

    private int code;
    private OrderStatusEnum orderStatusEnum;
    private String yfCode;
    private String desc;

    TransactionStatusEnum(int code, OrderStatusEnum orderStatusEnum, String yfCode, String desc) {
        this.code = code;
        this.orderStatusEnum = orderStatusEnum;
        this.yfCode = yfCode;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public OrderStatusEnum getOrderStatusEnum() {
        return orderStatusEnum;
    }

    public String getYfCode() {
        return yfCode;
    }

    public String getDesc() {
        return desc;
    }

    public static TransactionStatusEnum getTransactionStatus(FundChannelEnum fundChannelEnum, String yfCode) {
        TransactionStatusEnum result = null;
        if (StringUtils.isEmpty(yfCode)) {
            return result;
        }
        if (fundChannelEnum == FundChannelEnum.YIFENG) {
            TransactionStatusEnum[] transactionStatusEnumArray = TransactionStatusEnum.values();
            for (TransactionStatusEnum transactionStatusEnum : transactionStatusEnumArray) {
                if (StringUtils.isEmpty(transactionStatusEnum.getYfCode())) {
                    continue;
                }
                if (StringUtils.equals(yfCode, transactionStatusEnum.getYfCode())) {
                    result = transactionStatusEnum;
                    break;
                }
            }
        }
        return result;
    }

    public static TransactionStatusEnum getTransactionStatus(FundChannelEnum fundChannelEnum, int code) {
        TransactionStatusEnum result = null;
        if (fundChannelEnum == FundChannelEnum.YIFENG) {
            TransactionStatusEnum[] transactionStatusEnumArray = TransactionStatusEnum.values();
            for (TransactionStatusEnum transactionStatusEnum : transactionStatusEnumArray) {
                if (transactionStatusEnum.getCode() == 0) {
                    continue;
                }
                if (code == transactionStatusEnum.getCode()) {
                    result = transactionStatusEnum;
                    break;
                }
            }
        }
        return result;
    }

}
