package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

/**
 * 计划持仓组合关系
 */
@Data
@ToString
public class PlanPortfolioRelDO extends BaseBean{

    private Long planPortfolioRelId;

    private String userId;

    private Long planId;

    private Long mainModelId;

    private String thirdPortfolioId;

    private Integer channel;

}