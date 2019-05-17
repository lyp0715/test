package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.api.enums.order.TransactionTypeEnum;
import com.snb.deal.biz.invest.InvestBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 投资订单回调
 */
@Slf4j
public class InvestOrderCallbackConsumer extends ReliabilityEventMessageHandlerAdaptor<OrderCallbackMessage> {

    @Resource
    InvestBiz investBiz;

    @Override
    protected void handleData(OrderCallbackMessage message) throws Exception {

        log.info("收到订单回调消息，message={}",message);

        if (Objects.isNull(message) || StringUtils.isBlank(message.getMerchantNumber())
                || StringUtils.isBlank(message.getTransactionType()) || Objects.isNull(message.getChannel())) {
            return;
        }

        if (message.getTransactionType().equals(TransactionTypeEnum.AUTO_INVEST.getType()) ||
                message.getTransactionType().equals(TransactionTypeEnum.BUY.getType())) {

            investBiz.syncInvestOrder(message.getMerchantNumber(),message.getChannel());

        }

    }
}
