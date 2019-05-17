package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 基金调仓概要信息明细-DO
 * @author RunFa.Zhou
 * @date 2018-04-19
 * @return
 */
@Data
@ToString
@EqualsAndHashCode
public class OrderRebalanceSummaryDetailDO extends BaseBean {


    /**
     * 基金调仓概要明细信息Id
     */
    private Long orderRebalanceSummaryDetailId;

    /**
     * 基金调仓概要ID
     */
    private Long orderRebalanceSummaryId;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 交易费用
     */
    private BigDecimal transactionCharge;

    /**
     * 交易类型 1赎回2申购
     */
    private Integer transactionType;

    /**
     * 调仓金额
     */
    private BigDecimal investmentAmount;

    /**
     * 调仓份额
     */
    private BigDecimal investmentUnits;

    /**
     * 调仓后的基金占比,单位为1%，精确到0.01，如1.50，即表示1.50%
     */
    private BigDecimal postProportion;

    /**
     * 调仓前的基金占比,单位为1%，精确到0.01，如1.50，即表示1.50%
     */
    private BigDecimal preProportion;

    /**
     * 基金类型
     */
    private String fundType;

    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;

}