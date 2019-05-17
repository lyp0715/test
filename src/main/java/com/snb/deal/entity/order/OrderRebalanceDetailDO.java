package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 基金调仓订单明细信息-DO
 * @author RunFa.Zhou
 * @date 2018-04-19
 * @return
 */
@Data
@ToString
@EqualsAndHashCode
public class OrderRebalanceDetailDO extends BaseBean{

    String userId;

    /**
     * 调仓订单明细id
     */
    private Long orderRebalanceDetailId;

    /**
     * 调仓订单Id
     */
    private Long orderRebalanceId;

    /**
     * 子订单流水号
     */
    private String contractNumber;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金简称
     */
    private String fundName;

    /**
     * 申购金额
     */
    private BigDecimal investAmount;

    /**
     * 下单日期
     */
    private Date orderDate;

    /**
     * 下单失败时，返回失败原因
     */
    private String reason;

    /**
     * 赎回份额，如为申购，这个字段为空值
     */
    private BigDecimal redemptionUnit;

    /**
     * 清算日期
     */
    private Date settlementDate;

    /**
     * 基金申购确认日差；如返回2，即是需要2个工作日确认成功；transactionType=buy时返回
     */
    private Integer transactionCfmLagDay;

    /**
     * 交易日期
     */
    private Date transactionDate;

    /**
     * 基金赎回清算日差；如返回2，即是需要2个工作日资金到账；transactionType=sell时返回
     */
    private Integer transactionSettleLagDay;

    /**
     * 调仓订单下单成功的时候，卖的订单是下单成功（received），买的订单是待付款（pending.payment）
     */
    private Integer transactionStatus;

    /**
     * 交易类型 1赎回 2申购
     */
    private Integer transactionType;

    /**
     * 未打折的交易费率。单位为1%，2位小数，如1.50，即表示1.50%与订单状态尚未确认（priced、completed、ipo.processing），返回的是估算费率；若已确认成功，则返回的是实际费率
     */
    private BigDecimal transactionRate;

    /**
     * 折扣率，精确到0.0001，如0.2000，表示20.00%，即2折
     */
    private BigDecimal discountRate;

    /**
     * 交易费用
     */
    private BigDecimal transactionCharge;

    /**
     * 银行代码
     */
    private String bankCode;

    /**
     * 账户号码
     */
    private String accountNumber;

    /**
     * 银行卡号
     */
    private String bankNumber;

    /**
     * 是否可以撤单(Y:是,N:否)
     */
    private String cancelEnable;

    /**
     * 撤单日期
     */
    private Date canceledDate;

    /**
     * 交易完成日期。一般为确认日期+1个自然日，标识交易已完成。如为1950-01-01或者空值，则订单尚未完成
     */
    private Date completedDate;

    /**
     * 折扣后的交易费用。与订单状态尚未确认（priced、completed、ipo.processing），返回的是估算费用；若已确认成功，则返回的是实际费用
     */
    private BigDecimal discountTransactionCharge;

    /**
     * 预计确认日期
     */
    private Date expectedConfirmedDate;

    /**
     * 交易代码.当为空时代表现金钱包交易。
     */
    private Integer investorPayId;

    /**
     * 交易方式.0=银行卡，1=现金钱包支付，2=线下转账,3=商户代扣
     */
    private Integer payMethod;

    /**
     * 确认日期。基金公司确认订单的日期，也是份额增加或减少的日期。如为1950-01-01或者空值，则订单尚未确认
     */
    private Date pricedDate;

    /**
     * 交易份额；如申购、认购、定投的订单尚未确认，返回空值；如赎回订单，则返回赎回份额
     */
    private BigDecimal transactionUnit;

    /**
     * 退款时间
     */
    private Date voidDate;


}