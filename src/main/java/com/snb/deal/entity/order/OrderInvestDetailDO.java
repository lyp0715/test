package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 投资订单明细
 */
@Data
@ToString
public class OrderInvestDetailDO extends BaseBean{

    /**
     * 业务主键ID
     */
    private Long orderInvestDetailId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 投资订单ID
     */
    private Long orderInvestId;

    /**
     * 交易流水号
     */
    private String merchantNumber;

    /**
     * 第三方交易流水号
     */
    private String contractNumber;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 是否能撤单 Y or N
     */
    private String cancelEnable;

    /**
     * 撤单日期
     */
    private Date canceledDate;

    /**
     * 银行卡编码
     */
    private String bankCode;

    /**
     * 银行卡号
     */
    private String bankNumber;

    /**
     * 交易金额
     */
    private BigDecimal transactionAmount;

    /**
     * 交易份额
     */
    private BigDecimal transactionUnit;

    /**
     * 未打折交易费率
     */
    private BigDecimal transactionRate;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 未打折交易费用
     */
    private BigDecimal transactionCharge;

    /**
     * 折扣后交易费用
     */
    private BigDecimal discountTransactionCharge;

    /**
     * 下单日期
     */
    private Date orderDate;

    /**
     * 交易日期
     */
    private Date transactionDate;

    /**
     * 交易确认日差
     */
    private Integer transactionCfmLagDay;

    /**
     * 参考价格
     */
    private BigDecimal transactionPrice;

    /**
     * 确认日期
     */
    private Date pricedDate;

    /**
     * 预计确认日期
     */
    private Date expectedConfirmedDate;

    /**
     * 完成日期
     */
    private Date completedDate;

    /**
     * 清算日期
     */
    private Date settlementDate;

    /**
     * 交易状态
     * @see com.snb.deal.enums.TransactionStatusEnum
     */
    private Integer transactionStatus;

    /**
     * 交易代码.当为空时代表现金钱包交易。
     */
    private Integer investorPayId;

    /**
     * 订单失败原因
     */
    private String reason;

    /**
     * 定投计划id
     */
    private Integer rspId;

    /**
     * 交易类型：buy：申购，rsp：定投
     */
    private String transactionType;

    /**
     * 组合代码
     */
    private String portfolioCode;

    /**
     * 持仓组合代码
     */
    private String portfolioId;

    /**
     * 退款时间
     */
    private Date voidDate;

}