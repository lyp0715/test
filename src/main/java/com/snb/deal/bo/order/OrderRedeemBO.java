package com.snb.deal.bo.order;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@ToString
public class OrderRedeemBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;
    /**
     * 用户id
     */
    private String userId;

    /**
     * 投资者的奕丰账户。在开户接口有返回给商户。
     */
    private String accountNumber;

    /**
     * 赎回的组合编码
     */
    private String portfolioCode;

    /**
     * 赎回的组合编码
     */
    private String portfolioId;

    /**
     * 组合赎回金额，2位小数，单位为元
     */
    private BigDecimal transactionAmount;

    /**
     * 投资者账户绑定的支付代码。当paymentMethod=0时，必须上传对应的支付代码。可通过《获取账户的支付方式》获取
     */
    private Integer investorPayId = 0;
}
