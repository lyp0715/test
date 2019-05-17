package com.snb.deal.entity.order;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 红利再投订单
 */
@Data
@ToString
public class OrderDividendDO {

    private Long id;

    private Long orderDividendId;

    private String userId;

    private String contractNumber;

    private String fundName;

    private String fundCode;

    private String merchantNumber;

    private String accountNumber;

    private Long orderNo;

    private Date orderDate;

    private Date pricedDate;

    private Integer transactionStatus;

    private Date transactionDate;

    private BigDecimal transactionUnit;

    private String thirdPortfolioId;

    private Integer channel;

    private Date createTime;

    private Date updateTime;

    private Integer yn;

}