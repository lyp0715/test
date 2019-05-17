package com.snb.deal.remote.callback;

import com.alibaba.dubbo.config.annotation.Service;
import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.api.remote.callback.IfastCallbackRemote;
import lombok.extern.slf4j.Slf4j;

/**
 * 奕丰回调服务
 */
@Service(version = "1.0")
@Slf4j
public class IfastCallbackRemoteImpl implements IfastCallbackRemote {

    /**
     * 自动定投
     * @param autoInvestMessage
     */
    @Event(reliability = true,
            eventType = "'ifastAutoInvestCallback'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.autoInvest.callBack",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void autoInvestCallback(AutoInvestMessage autoInvestMessage) {

        autoInvestMessage.setEventId(autoInvestMessage.getThirdPlanId()+"_"+autoInvestMessage.getMerchantNumber());

        EventMessageContext.addMessage(autoInvestMessage);
    }

    /**
     *  订单回调
     * @param orderCallbackMessage
     */
    @Event(reliability = true,
            eventType = "'ifastOrderCallback'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.order.callBack",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void orderCallback(OrderCallbackMessage orderCallbackMessage) {
        EventMessageContext.addMessage(orderCallbackMessage);
    }
}
