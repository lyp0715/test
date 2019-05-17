package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.api.enums.order.TransactionTypeEnum;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;

/**
 * 赎回订单回调
 */
@Slf4j
public class RedeemOrderCallbackConsumer extends ReliabilityEventMessageHandlerAdaptor<OrderCallbackMessage> {

    @Resource
    private OrderRedeemBiz orderRedeemBiz;

    @Override
    protected void handleData(OrderCallbackMessage message) throws Exception {

        if (StringUtils.equals(TransactionTypeEnum.REDEEM.getType(), message.getTransactionType())) {
            log.info("RedeemOrderCallbackConsumer-开始，merchantNumber:{}", StringUtils.defaultString(message.getMerchantNumber()));
            orderRedeemBiz.syncOrderRedeemCallBack(message.getMerchantNumber(), message.getChannel().getChannel());
            log.info("RedeemOrderCallbackConsumer-结束，merchantNumber:{}", StringUtils.defaultString(message.getMerchantNumber()));
        }
    }
}
