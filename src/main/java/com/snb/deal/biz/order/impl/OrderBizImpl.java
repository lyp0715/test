package com.snb.deal.biz.order.impl;

import com.google.common.base.Preconditions;
import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AccountSyncMessage;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.api.enums.order.TransactionTypeEnum;
import com.snb.deal.biz.order.OrderBiz;
import com.snb.deal.bo.rebalance.OrderRebalanceConditionBO;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import com.snb.deal.entity.order.OrderRedeemDO;
import com.snb.deal.enums.OrderBusinessEnum;
import com.snb.deal.mapper.order.OrderRebalanceMapper;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.order.OrderRedeemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class OrderBizImpl implements OrderBiz{

    @Resource
    private OrderInvestService orderInvestService;
    @Resource
    private OrderRedeemService orderRedeemService;
    @Autowired
    private OrderRebalanceMapper orderRebalanceMapper;

    @Event(reliability = true,
            eventType = "'syncAccount'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.account.sync",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void syncAccountAfterOrderChange(String userId, String accountNumber) {
        AccountSyncMessage message = new AccountSyncMessage();
        message.setUserId(userId);
        message.setAccountNumber(accountNumber);
        message.setChannel(FundChannelEnum.YIFENG);
        message.setEventId(userId+new Date().getTime());
        EventMessageContext.addMessage(message);
    }

    @Event(reliability = true,
            eventType = "'syncAccount'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.account.sync",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void afterOrderCallback(OrderCallbackMessage message) throws Exception {

        String transactionType = message.getTransactionType();
        String merchantNumber = message.getMerchantNumber();
        FundChannelEnum channel = message.getChannel();
        String userId = "";
        String accountNumber = "";

        if (transactionType.equals(TransactionTypeEnum.AUTO_INVEST.getType()) ||
                transactionType.equals(TransactionTypeEnum.BUY.getType())) {
            //投资订单
            //根据订单号查询投资订单
            OrderInvestDO orderInvestDO = orderInvestService.queryByMerchantNumber(merchantNumber,channel);
            Preconditions.checkNotNull(orderInvestDO,"根据流水号:%s渠道:%s查询投资订单失败",merchantNumber,channel);
            userId = orderInvestDO.getUserId();
            accountNumber = orderInvestDO.getAccountNumber();
        }else if (transactionType.equals(TransactionTypeEnum.REDEEM.getType())) {
            //赎回订单
            List<OrderRedeemDO> orderRedeemDOList = orderRedeemService.queryByMerchantNumber(merchantNumber, channel.getChannel());

            Preconditions.checkState(CollectionUtils.isNotEmpty(orderRedeemDOList),"根据流水号:%s渠道:%s查询赎回订单失败",merchantNumber,channel);

            OrderRedeemDO orderRedeemDO = orderRedeemDOList.get(0);

            userId = orderRedeemDO.getUserId();
            accountNumber = orderRedeemDO.getAccountNumber();

        } else if (transactionType.equals(TransactionTypeEnum.REBALANCE.getType())) {
            //调仓订单
            OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
            orderRebalanceConditionBO.setChannel(FundChannelEnum.YIFENG.getChannel());
            orderRebalanceConditionBO.setBusinessCode(OrderBusinessEnum.REBALANCE.getCode());
//            orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
            orderRebalanceConditionBO.setMerchantNumber(merchantNumber);

            //1.查询异步同步调仓订单请求集合
            OrderRebalanceDO orderRebalanceDO = orderRebalanceMapper
                    .queryOrderRebalanceDOByCondition(orderRebalanceConditionBO);

            Preconditions.checkNotNull(orderRebalanceDO,"根据订单号:%s渠道:%s查询调仓订单失败",merchantNumber,channel);

            userId = orderRebalanceDO.getUserId();
            accountNumber = orderRebalanceDO.getAccountNumber();
        } else {
            log.error("订单回调，无效的订单类型，{}",transactionType);
            return;
        }

        Preconditions.checkState(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(accountNumber),"根据订单号:%s渠道:%s查询用户和基金账户失败",merchantNumber,channel);

        AccountSyncMessage accountSyncMessage = new AccountSyncMessage();
        accountSyncMessage.setUserId(userId);
        accountSyncMessage.setAccountNumber(accountNumber);
        accountSyncMessage.setChannel(FundChannelEnum.YIFENG);
        accountSyncMessage.setEventId(userId+new Date().getTime());
        EventMessageContext.addMessage(accountSyncMessage);

    }
}
