package com.snb.deal.entity.order;


import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@ToString(callSuper=true, includeFieldNames=true)
public class OrderRebalanceDO extends BaseBean {

    /**
     * 业务主键id
     */
    Long orderRebalanceId;

    /**
     * 用户id
     */
    String userId;

    /**
     * 订单号
     */
    Long orderNo;

    /**
     * 投资在第三方帐号ID
     */
    String accountNumber;

    /**
     * 调仓订单交易状态
     */
    Integer transactionStatus;

    /**
     * 交易费用
     */
    BigDecimal transactionCharge;

    /**
     * (拾年保)上送-交易流水号
     */
    String merchantNumber;

    /**
     * 投资组合代码
     */
    private String portfolioCode;

    /**
     * 第三方机构-用户持仓组合编码ID
     */
    String thirdPortfolioId;

    /**
     * 响应编码
     */
    String responseCode;

    /**
     *响应详情
     */
    String responseMessage;

    /**
     * 渠道
     */
    Integer channel;

}
