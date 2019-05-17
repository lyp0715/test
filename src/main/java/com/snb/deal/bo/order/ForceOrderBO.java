package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单信息
 */
@Data
@ToString
public class ForceOrderBO implements Serializable{
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 账户号码
     */
    private String accountNumber;

    /**
     * 是否可以撤单
     */
    private String cancelable;

    /**
     * 撤单日期
     */
    private String canceledDate;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 商户订单流水号
     */
    private String merchantNumber;

    /**
     * 下单日期
     */
    private String orderDate;

    /**
     * 投资者账户绑定的支付代码。当paymentMethod=0时，必须上传对应的支付代码。可通过《获取账户的支付方式》获取
     */
    private Integer investorPayId = 0;

    /**
     * 赎回的组合编码
     */
    private String portfolioCode;

    /**
     * 组合编码
     */
    private String portfolioId;

    /**
     * 确认日期
     */
    private String pricedDate;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 如果订单为定投计划生成的订单，则返回关联的定投计划；其他则为空值
     */
    private Integer rspId;

    /**
     * 申购、认购、定投的订单的总购买金额（包含购买费用）；赎回订单的总赎回金额（包含赎回费用），如赎回订单未确认，返回空值
     */
    private String transactionAmount;

    /**
     * 未打折的交易费用。若订单状态尚未确认，返回的是估算费用；若已确认成功，则返回的是实际费用
     */
    private String transactionCharge;

    /**
     * 交交易日期。为奕丰向基金公司下单的日期，如为1950-01-01或者空值，则订单尚未下单成功（向奕丰系统）
     */
    private String transactionDate;

    /**
     * 未打折的交易费率。单位为1%，2位小数，如1.50，即表示1.50%与订单状态尚未确认，返回的是估算费率；若已确认成功，则返回的是实际费率
     */
    private String transactionRate;

    /**
     * 交易状态(failure：支付失败； received：下单成功； priced：确认成功； completed：交易成功；ipo.processing：认购确认成功；
     * pending.payment：等待付款；canceling：撤单中；
     * canceled：已撤单；pending.void：等待退款；void：交易失败；payment：支付过程中的过渡状态，前台请过滤这个状态)
     */
    private String transactionStatus;

    /**
     * buy：申购；ipo：认购；sell：赎回；rsp：定投；dividend：红利再投；
     * intra.swtich：同公司转换；inter.switch：跨公司转换；rapid.sell：快速取现；
     * sell.force：强制赎回；units.increment：份额强增；units.decrement：份额强减
     */
    private String transactionType;

    /**
     * 交易份额；如申购、认购、定投的订单尚未确认，返回空值；如赎回订单，则返回赎回份额
     */
    private String transactionUnit;

}
