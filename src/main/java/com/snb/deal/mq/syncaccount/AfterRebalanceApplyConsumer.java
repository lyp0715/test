package com.snb.deal.mq.syncaccount;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.RebalanceOrderMessage;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 调仓发起成功后，同步用户账户
 */
@Slf4j
public class AfterRebalanceApplyConsumer extends ReliabilityEventMessageHandlerAdaptor<RebalanceOrderMessage> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(RebalanceOrderMessage rebalanceOrderMessage) throws Exception {

        log.info("调仓订单发起成功，同步用户账户：{}",rebalanceOrderMessage);

        if (Objects.isNull(rebalanceOrderMessage)) {
            return;
        }

        String userId = rebalanceOrderMessage.getUserId();
        String accountNumber = rebalanceOrderMessage.getAccountNumber();

        if (StringUtils.isBlank(userId) || StringUtils.isBlank(accountNumber)) {
            log.error("调仓订单发起成功，同步用户账户，数据异常：{}",rebalanceOrderMessage);
            return;
        }

        orderBiz.syncAccountAfterOrderChange(userId,accountNumber);
    }
}
