package com.snb.deal.bo.plan;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class PlanIncomeBO {
    /**
     *  组合累计收益
     */
    private BigDecimal totalIncome = BigDecimal.ZERO;
}
