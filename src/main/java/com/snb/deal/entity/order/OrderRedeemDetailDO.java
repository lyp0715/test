package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString(callSuper = true, includeFieldNames = true)
public class OrderRedeemDetailDO extends BaseBean {

    /**
     * 业务主键
     */
    private Long orderRedeemDetailId;
    /**
     * orderRedeem业务主键
     */
    private Long orderRedeemId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 账户号码
     */
    private String accountNumber;
    /**
     * 银行代码
     */
    private String bankCode;
    /**
     * 银行卡号
     */
    private String bankNumber;
    /**
     * 是否可以撤单(Y:是,N:否)
     */
    private String cancelEnable;
    /**
     * 撤单日期。如未撤单，则为1950-01-01或者空值
     */
    private Date canceledDate;
    /**
     * 交易完成日期
     */
    private Date completedDate;
    /**
     * 交易流水号
     */
    private String contractNumber;
    /**
     * 折扣率，精确到0.0001，如0.2000，表示20.00%，即2折
     */
    private BigDecimal discountRate;
    /**
     * 折扣后的交易费用
     */
    private BigDecimal discountTransactionCharge;
    /**
     * 赎回基金交易预计确认日期
     */
    private Date expectedConfirmedDate;
    /**
     * 基金代码
     */
    private String fundCode;
    /**
     * 基金简称
     */
    private String fundName;
    /**
     * 交易代码.当为空时代表现金钱包交易。
     */
    private Integer investorPayId;
    /**
     * 交易流水号，命名规则为：商户代码+YYYYMMDD(日期)+5位序列号；保证每个订单每个商户唯一
     */
    private String merchantNumber;
    /**
     * 下单日期
     */
    private Date orderDate;
    /**
     * 交易方式.0=银行卡，1=现金钱包支付，2=线下转账,3=商户代扣
     */
    private Integer payMethod;
    /**
     * 赎回的组合编码
     */
    private String portfolioId;
    /**
     * 确认日期
     */
    private Date pricedDate;
    /**
     * 下单失败时，返回失败原因
     */
    private String reason;
    /**
     * 如果订单为定投计划生成的订单，则返回关联的定投计划；其他则为空值
     */
    private Integer rspId;
    /**
     * 清算日期。为奕丰和基金公司清算资金的日期。如为1950-01-01或者空值，则订单尚未清算。
     */
    private Date settlementDate;
    /**
     * 交易金额
     */
    private BigDecimal transactionAmount;
    /**
     * 交易费用
     */
    private BigDecimal transactionCharge;
    /**
     * 交易日期
     */
    private Date transactionDate;
    /**
     * 参考价格
     */
    private BigDecimal transactionPrice;
    /**
     * 未打折的交易费率。单位为1%，2位小数，
     * 如1.50，即表示1.50%与订单状态尚未确认（priced、completed、ipo.processing），
     * 返回的是估算费率；若已确认成功，则返回的是实际费率
     */
    private BigDecimal transactionRate;
    /**
     * 交易状态
     * {@link com.snb.deal.enums.TransactionStatusEnum}
     */
    private Integer transactionStatus;
    /**
     * 交易类型(buy：申购；ipo：认购；sell：赎回；rsp：定投；dividend：红利再投；intra.swtich：同公司转换；inter.switch：跨公司转换；rapid.sell：快速取现)
     */
    private Integer transactionType;
    /**
     * 赎回份额
     */
    private BigDecimal transactionUnit;
    /**
     * 退款时间
     */
    private Date voidDate;
    /**
     * 到帐日期
     */
    private Date expectedDealDate;
    /**
     * 赎回的组合编码
     */
    private String portfolioCode;
}