package com.snb.deal.bo.rebalance;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderRebalanceSummaryConditionBO {
    /**
     * 调仓订单号
     */
    Long orderRebalaceId ;

    /**
     * 交易类型
     */
    Integer transactionType;
    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;

    /**
     * 调仓订单概要Id
     */
    Long orderRebalanceSummaryId;
}
