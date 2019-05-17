package com.snb.deal.bo.plan;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Data
@ToString
public class PortfolioAccountBO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     *  组合累计收益
     */
    private BigDecimal totalIncome = BigDecimal.ZERO;

    /**
     *  组合昨日收益
     */
    private BigDecimal yesterdayIncome = BigDecimal.ZERO;

    /**
     * 组合可用金额
     */
    private BigDecimal availableAmount = BigDecimal.ZERO;

    /**
     * 组合可用份额
     */
    private BigDecimal availableUnit = BigDecimal.ZERO;

    /**
     * 组合累计投资金额（组合成本）
     */
    private BigDecimal totalInvestmentAmount = BigDecimal.ZERO;

    /**
     * 持有组合下的在途资产总值
     */
    private BigDecimal totalIntransitAmount = BigDecimal.ZERO;

    /**
     * 组合最高赎回金额
     */
    private BigDecimal maxRedemptionAmount = BigDecimal.ZERO;

    /**
     * 组合最低赎回金额
     */
    private BigDecimal minRedemptionAmount = BigDecimal.ZERO;

    /**
     * 组合最低保留金额
     */
    private BigDecimal minRetainAmount = BigDecimal.ZERO;

    /**
     * 组合类型
     */
    private String portfolioType;

    /**
     * 是否可以自由控制 Y=是，N=否
     */
    private String rebalanceEnable;

    /**
     * 是否可以赎回 Y=是，N=否
     */
    private String sellEnable;

    /**
     * 计划组合账户详情
     */
    List<PortfolioAccountDetailBO> portfolioAccountDetailBOList = Lists.newArrayList();

}
