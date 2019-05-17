package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 买入订单：组合费率和组合折后费率BO类
 *
 * @author yunpeng.zhang
 */
@Data
@ToString
public class OrderInvestCombinationRateBO {
    /**
     * 组合费率
     */
    private BigDecimal combinationRate;
    /**
     * 组合折后费率
     */
    private BigDecimal combinationDiscountedRate;
}
