package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ToString
public class OrderFeeRateBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 费率
     */
    private BigDecimal feeRate;
}
