package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class PlanStatisticsDO extends BaseBean{

    private Long planStatisticsId;

    private String userId;

    private Long planId;

    private BigDecimal totalInvestAmount = BigDecimal.ZERO;

    private Long totalSuccessNum = 0l;

    private Long totalFailedNum = 0l;

    private BigDecimal totalRedeemAmount = BigDecimal.ZERO;

}