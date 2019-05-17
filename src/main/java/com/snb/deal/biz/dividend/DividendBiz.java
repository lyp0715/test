package com.snb.deal.biz.dividend;

import com.snb.deal.api.dto.order.OrderDividendSyncRequest;

/**
 * @Description 红利再投
 * @author lizengqiang
 * @date 2018/5/25 14:18
 */
public interface DividendBiz {

    /**
     * 同步用户红利再投订单
     * @param request
     */
    void syncDividendOrder(OrderDividendSyncRequest request);

}
