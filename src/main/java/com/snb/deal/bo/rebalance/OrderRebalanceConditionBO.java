package com.snb.deal.bo.rebalance;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderRebalanceConditionBO {

    /**
     * 交易状态
     */
    Integer transactionStatus;

    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;

    /**
     * 业务码
     */
    Integer businessCode;
    /**
     * 订单号
     */
    Long orderNo;
    /**
     * 交易类型
     */
    Integer transactionType;

    /**
     * 调仓订单Id
     */
    Long orderRebalanceId;

    /**
     * 交易流水号
     */
    String merchantNumber;

}

