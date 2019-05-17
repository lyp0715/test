package com.snb.deal.mq.syncaccount;

import com.jianlc.event.EventMessageHandlerAdaptor;
import com.snb.common.mq.bean.InvestOrderSyncCompleteMessage;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 投资订单同步完成后，同步账户
 */
@Slf4j
public class AfterInvestOrderSyncConsumer extends EventMessageHandlerAdaptor<InvestOrderSyncCompleteMessage> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(InvestOrderSyncCompleteMessage message) throws Exception {
        log.info("投资订单同步后，同步用户账户：{}",message);

        try {
            orderBiz.syncAccountAfterOrderChange(message.getUserId(),message.getAccountNumber());
        } catch (Exception e) {
            log.error("投资订单同步后，同步用户账户异常",e);
        }
    }
}
