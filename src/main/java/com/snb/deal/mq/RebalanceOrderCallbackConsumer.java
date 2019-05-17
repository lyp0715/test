package com.snb.deal.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.OrderCallbackMessage;
import com.snb.deal.api.enums.order.TransactionTypeEnum;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.rebalance.OrderRebalanceAsyncBO;
import com.snb.deal.bo.rebalance.OrderRebalanceConditionBO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import com.snb.deal.enums.OrderBusinessEnum;
import com.snb.deal.enums.OrderStatusEnum;
import com.snb.deal.mapper.order.OrderRebalanceMapper;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;

/**
 * 调仓订单回调
 */
@Slf4j
public class RebalanceOrderCallbackConsumer extends ReliabilityEventMessageHandlerAdaptor<OrderCallbackMessage> {

    @Resource
    OrderRebalanceMapper orderRebalanceMapper;

    @Resource
    FundRebalanceBiz fundRebalanceBiz;

    @Reference(version = "1.0")
    FundUserRemote fundUserRemote;

    @Override
    protected void handleData(OrderCallbackMessage message) throws Exception {

        if(!"rebalance".equals(TransactionTypeEnum.REBALANCE.getType())){
            log.info("调仓业务回调处理,TransactionType不符合,退出处理！");
            return ;
        }

        log.info("收到调仓业务回调消息-{}",JSONObject.toJSONString(message));
        String merchantNumber = message.getMerchantNumber();

        if(StringUtils.isEmpty(merchantNumber)){
            log.info("调仓业务回调处理,MerchantNumber={},退出处理！",merchantNumber);
            return ;
        }

        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
        orderRebalanceConditionBO.setChannel(FundChannelEnum.YIFENG.getChannel());
        orderRebalanceConditionBO.setBusinessCode(OrderBusinessEnum.REBALANCE.getCode());
        orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
        orderRebalanceConditionBO.setMerchantNumber(merchantNumber);

        //1.查询异步同步调仓订单请求集合
        OrderRebalanceDO orderRebalanceDO = orderRebalanceMapper
                .queryOrderRebalanceDOByCondition(orderRebalanceConditionBO);

        if(orderRebalanceDO == null){
            log.info("调仓业务回调处理,通过条件查询调仓同步信息列表空,退出处理！查询条件={}",
                    JSONObject.toJSONString(orderRebalanceConditionBO));
            return ;
        }


        int channel = orderRebalanceDO.getChannel();// 渠道
        BaseFundRequest request = new BaseFundRequest();
        request.setFundPlatform(FundChannelEnum.getByChannel(channel));
        request.setUserId(orderRebalanceDO.getUserId());
        APIResponse<GetUserFundAccountInfoResponse> userAccountAPIResponse =
                fundUserRemote.getUserFundAccountInfo(request);// 查询用户基金账户信息
        GetUserFundAccountInfoResponse userAccountAPIResponseData = userAccountAPIResponse.getData();// 用户账户信息

        if(userAccountAPIResponseData == null || userAccountAPIResponse.getData() == null){
            log.info("调仓业务回调处理,查询用户基金账户信息异常,退出处理！查询条件={},OrderRebalanceConditionBO={}",
                    JSONObject.toJSONString(request),JSONObject.toJSONString(orderRebalanceConditionBO));
            return ;
        }

        OrderRebalanceAsyncBO orderRebalanceAsyncBO = new OrderRebalanceAsyncBO();
        orderRebalanceAsyncBO.setChannel(orderRebalanceDO.getChannel());
        orderRebalanceAsyncBO.setMerchantNumber(orderRebalanceDO.getMerchantNumber());
        orderRebalanceAsyncBO.setOrderNo(orderRebalanceDO.getOrderNo());
        orderRebalanceAsyncBO.setOrderRebanlanceId(orderRebalanceDO.getOrderRebalanceId());
        orderRebalanceAsyncBO.setUserId(orderRebalanceDO.getUserId());
        orderRebalanceAsyncBO.setAccountNumber(userAccountAPIResponse.getData().getAccountNumber());

        // 调仓订单同步
        fundRebalanceBiz.orderRebalanceAsync(orderRebalanceAsyncBO);

    }
}
