package com.snb.deal.mq.syncaccount;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.biz.order.OrderBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 订单回调后，同步用户账户
 */
@Slf4j
public class AfterOrderCallbackConsumer extends ReliabilityEventMessageHandlerAdaptor<OrderCallbackMessage> {

    @Resource
    private OrderBiz orderBiz;

    @Override
    protected void handleData(OrderCallbackMessage message) throws Exception {

        log.info("订单回调后，同步用户账户：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        if (StringUtils.isBlank(message.getTransactionType())
                || StringUtils.isBlank(message.getMerchantNumber()) || Objects.isNull(message.getChannel())) {
            log.error("订单回调后，同步账户失败，消息对象异常：{}",message);
            return;
        }

        orderBiz.afterOrderCallback(message);

    }
}
