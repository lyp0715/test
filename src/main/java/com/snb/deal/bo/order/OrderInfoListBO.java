package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description 
 * @author lizengqiang
 * @date 2018/4/27 16:22
 */
@Data
@ToString(callSuper = true, includeFieldNames = true)
public class OrderInfoListBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;

    /**
     * 交易类型
     */
    private Integer businessCode;
    /**
     * 购买下单时间
     */
    private Date sendTime;

    /**
     * 交易金额
     */
    private BigDecimal transactionAmount;

    /**
     * 交易费用
     */
    private BigDecimal transactionCharge;
    /**
     * 银行卡号
     */
    private String bankNumber;

    /**
     * 银行卡名称
     */
    private String bankName;

    /**
     * 银行卡logo
     */
    private String bankIcon;
    /**
     * 订单状态
     */
    private Integer orderStatus;

    /**
     * 交易时间
     */
    private Date transactionDate;

    /**
     * 预计确认时间
     */
    private Date expectPricedDate;
    /**
     * 完成时间
     */
    private Date completedDate;


    /**
     * 调仓比例
     */
    private BigDecimal rebalanceRate;

    /**
     * 交易份额
     */
    private BigDecimal transactionUnit;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 确认份额时间
     */
    private Date pricedDate;

    /**
     * 实际扣款金额
     */
    private BigDecimal actualTransactionAmount;

}

