package com.snb.deal.entity.portfolio;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 组合每日收益
 */
@Data
@ToString
public class PortfolioDailyIncomeDO extends BaseBean{

    private Long userPortfolioDailyIncomeId;

    private String userId;

    private Long planPortfolioRelId;

    /**
     * 累计收益率
     */
    private BigDecimal accumulatedPerformance;

    /**
     * 累计收益
     */
    private BigDecimal accumulatedProfitloss;

    /**
     * 日涨幅
     */
    private BigDecimal performanceDaily;

    /**
     * 当日收益
     */
    private BigDecimal profitlossDaily;

    /**
     * 产生收益日期
     */
    private Date showedDate;

    /**
     * 收益更新日期
     */
    private Date updatedDate;

}