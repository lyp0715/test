package com.snb.deal.bo.order;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@ToString
public class OrderRedeemAsyncBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;

    /**
     * 业务主键
     */
    private Long orderRedeemId;

    /**
     * 用户id
     */
    private String userId;
    /**
     * 订单号
     */
    private Long orderNo;
    /**
     * 交易流水号，命名规则为：商户代码+YYYYMMDD(日期)+5位序列号；保证每个订单每个商户唯一
     */
    private String merchantNumber;

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
     * 发送时间
     */
    private Date createTime;
}
