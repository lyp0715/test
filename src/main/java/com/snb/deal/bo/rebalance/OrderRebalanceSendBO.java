package com.snb.deal.bo.rebalance;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderRebalanceSendBO {

    /**
     * 调仓订单号
     */
    Long orderRebalanceId;


    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;

    /**
     * 第三方账户号
     */
    String thirdAccountNumber;

    /**
     * 基金编码
     */
    String thirdPortfolioCode;

    /**
     * 编码持仓Id
     */
    String thirdPortfolioId;

    /**
     * 发送流水号
     */
    String merchantNumber;


    /**
     * 订单号
     */
    Long orderNo;
}
