package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 计划持仓账户
 */
@Data
@ToString
public class PlanPortfolioAccountDO extends BaseBean {
    /**
     * 业务id
     */
    private Long planPortfolioAccountId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 计划持仓关系id
     */
    private Long planPortfolioRelId;
    /**
     * 总收益率
     */
    private BigDecimal totalPerformance;
    /**
     * 总收益
     */
    private BigDecimal totalIncome;
    /**
     * 总资产
     */
    private BigDecimal totalAmount;
    /**
     * 资产成本
     */
    private BigDecimal totalInvestAmount;
    /**
     * 总在途资产
     */
    private BigDecimal totalIntransitAmount;
    /**
     * 可赎回资产
     */
    private BigDecimal availableAmount;
    /**
     * 可赎回份额
     */
    private BigDecimal availableUnit;

    /**
     * 不可用资产
     */
    private BigDecimal unAvailableAmount;

    /**
     * 最大可赎回金额
     */
    private BigDecimal maxRedeemableAmount;

    /**
     * 最小可赎回金额
     */
    private BigDecimal minRedeemableAmount;

    /**
     * 最小保留金额
     */
    private BigDecimal minRetainAmount;

}