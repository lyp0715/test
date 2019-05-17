package com.snb.deal.biz.order;

import com.snb.common.mq.bean.OrderCallbackMessage;

public interface OrderBiz {

    /**
     * 订单更新后，同步用户账户
     * @param userId 用户ID
     * @param accountNumber 基金账户
     */
    void syncAccountAfterOrderChange(String userId, String accountNumber);

    /**
     * 订单回调
     * @param message
     * @throws Exception
     */
    void afterOrderCallback(OrderCallbackMessage message) throws Exception;
}
