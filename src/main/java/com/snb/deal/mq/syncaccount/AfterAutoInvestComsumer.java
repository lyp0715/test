package com.snb.deal.mq.syncaccount;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.AutoInvestCompleteMessage;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 定投完成后同步用户账户
 */
@Slf4j
public class AfterAutoInvestComsumer extends ReliabilityEventMessageHandlerAdaptor<AutoInvestCompleteMessage> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(AutoInvestCompleteMessage autoInvestCompleteMessage) throws Exception {
      log.info("定投完成，同步用户账户：{}",autoInvestCompleteMessage);
      try {
          String userId = autoInvestCompleteMessage.getUserId();
          String accountNumber = autoInvestCompleteMessage.getAccountNumber();

          orderBiz.syncAccountAfterOrderChange(userId,accountNumber);

      } catch (Exception e) {
          log.error("定投完成，同步用户账户异常",e);
      }

    }
}
