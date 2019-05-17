package com.snb.deal.bo.rebalance;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderRebalanceCreateBO {

    /**
     * 用户Id
     */
    String userId;

    /**
     * 用户模型Id
     */
    Long fundMainModelId;

    /**
     * 三方持仓编码Id
     */
    String thirdPortfolioId;

    /**
     * 计划持仓Id
     */
    Long planPortfolioRelId;

    /**
     * 组合编码
     */
    String thirdPortfolioCode;

    /**
     * 渠道
     */
    Integer channel;

    /**
     * 流水号
     */
    String merchantNumber;

    /**
     * 关联的第三方账户号
     */
    String thirdAccountNumber;

    /**
     * 计划Id
     */
    Long planId;

}
