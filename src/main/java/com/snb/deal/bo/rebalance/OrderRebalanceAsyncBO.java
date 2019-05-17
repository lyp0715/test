package com.snb.deal.bo.rebalance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@NoArgsConstructor
@ToString
public class OrderRebalanceAsyncBO implements Serializable {


    /**
     * 用户的奕丰账户,用户的奕丰账户,在开户接口有返回给商户(在开户时有返回给商户)
     */
    String accountNumber;

    /**
     * 要查询的订单流水编号(如提供，则返回过滤后的数据)
     */
    String merchantNumber;

    /**
     * 用户Id
     */
    String userId;

    /**
     * 订单号
     */
    Long orderNo;


    /**
     * 调仓订单Id
     */
    Long orderRebanlanceId;

    /**
     * 渠道
     */
    Integer channel;
    /**
     * 组合持仓编码
     */
    String thirdPortfolioId;
}
