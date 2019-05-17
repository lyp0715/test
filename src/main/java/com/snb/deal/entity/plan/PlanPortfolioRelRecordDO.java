package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

/**
 * 计划持仓组合关系修改记录
 */
@Data
@ToString
public class PlanPortfolioRelRecordDO extends BaseBean {

    private Long palnPortfolioRelRecordId;

    private String userId;

    private String planId;

    private String thirdPortfolioCode;

    private String thirdPortfolioId;

    private Boolean thirdType;

}