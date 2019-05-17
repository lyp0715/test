package com.snb.deal.mq.syncaccount;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.OrderRedeemSyncNotify;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 赎回发起成功后，同步用户账户
 */
@Slf4j
public class AfterRedeemApplyConsumer extends ReliabilityEventMessageHandlerAdaptor<OrderRedeemSyncNotify> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(OrderRedeemSyncNotify orderRedeemSyncNotify) throws Exception {

        log.info("赎回订单发起成功，同步用户账户：{}",orderRedeemSyncNotify);

        if (Objects.isNull(orderRedeemSyncNotify)) {
            return;
        }

        String userId = orderRedeemSyncNotify.getUserId();
        String accountNumber = orderRedeemSyncNotify.getAccountNumber();

        if (StringUtils.isBlank(userId) || StringUtils.isBlank(accountNumber)) {
            log.error("赎回订单发起成功，同步用户账户，数据异常：{}",orderRedeemSyncNotify);
            return;
        }
        orderBiz.syncAccountAfterOrderChange(userId,accountNumber);
    }
}
