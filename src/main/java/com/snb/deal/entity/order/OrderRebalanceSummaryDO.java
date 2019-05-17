package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 基金调仓概要信息-DO
 * @author RunFa.Zhou
 * @date 2018-04-19
 * @return
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class OrderRebalanceSummaryDO extends BaseBean {

    /**
     * 调仓概要Id
     */
    private Long orderRebalanceSummaryId;

    /**
     * 调仓订单
     */
    private Long orderRebalanceId;

    /**
     * 调整原因
     */
    private String ableReason;

    /**
     * 调整失败原因
     */
    private String disableReson;

    /**
     * 调整费用(总和)
     */
    private BigDecimal totalFee;
    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;


}