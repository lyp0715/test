package com.snb.deal.mq.syncaccount;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.InvestOrderMessage;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 投资订单发起完成，同步账户
 */
@Slf4j
public class AfterInvestApplyConsumer extends ReliabilityEventMessageHandlerAdaptor<InvestOrderMessage> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(InvestOrderMessage investOrderMessage) throws Exception {
        log.info("单笔买入发起完成，同步用户账户：{}",investOrderMessage);
        try {
            orderBiz.syncAccountAfterOrderChange(investOrderMessage.getUserId(),investOrderMessage.getAccountNumber());
        }catch (Exception e) {
            log.info("单笔买入发起完成，同步用户账户异常",e);
        }
    }
}
