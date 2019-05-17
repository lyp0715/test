package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 投资订单
 */
@Data
@ToString
public class OrderInvestDO extends BaseBean{

    /**
     * 业务主键ID
     */
    private Long orderInvestId;

    /**
     * 用户id
     */
    private String userId;

    /**
     *  第三方交易流水号
     */
    private String contractNumber;

    /**
     * 交易流水号
     */
    private String merchantNumber;

    /**
     * 第三方账号
     */
    private String accountNumber;

    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 组合代码
     */
    private String portfolioCode;

    /**
     * 交易金额
     */
    private BigDecimal transactionAmount;

    /**
     * 交易状态
     * @see com.snb.deal.enums.TransactionStatusEnum
     */
    private Integer transactionStatus;

    /**
     * 交易手续费
     */
    private BigDecimal transactionCharge;

    /**
     * 响应码
     */
    private String responseCode;

    /**
     * 响应信息
     */
    private String responseMessage;

    /**
     * 投资订单类型
     * @see com.snb.deal.api.enums.order.InvestTypeEnum
     */
    private Integer investType;

    /**
     * 第三方持仓组合代码
     */
    private String thirdPortfolioId;

    /**
     * 订单渠道
     * @see com.snb.common.enums.FundChannelEnum
     */
    private Integer channel;

}