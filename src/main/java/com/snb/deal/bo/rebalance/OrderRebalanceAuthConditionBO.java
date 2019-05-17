package com.snb.deal.bo.rebalance;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class OrderRebalanceAuthConditionBO {
    /**
     * 用户Id
     */
    String userId;

    /**
     * 渠道
     */
    Integer channel;

    /**
     * 授权状态
     */
    Integer authStatus;

    /**
     * 调仓订单号
     */
    Long orderRebalanceId;

    /**
     * 开始时间
     */
    Date startTime;

    /**
     * 结束时间
     */
    Date endTime;
}
