package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 计划执行记录
 */
@Data
@ToString
public class PlanChangeRecordDO extends BaseBean{

    private Long planchangeRecordId;
    private Long planInfoId;
    private String userId;
    private String thirdPlanId;
    private Integer channel;
    private String planName;
    private Integer cycle;
    private Integer cycleDay;
    private BigDecimal portfolioAmount;

}