package com.snb.deal.biz.redeem.impl;

import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.mq.bean.OrderRedeemSyncNotify;
import com.snb.deal.api.dto.redeem.OrderRedeemResponse;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import com.snb.deal.bo.order.ForceOrderBO;
import com.snb.deal.bo.order.OrderRedeemAsyncBO;
import com.snb.deal.bo.order.OrderRedeemBO;
import com.snb.deal.entity.order.OrderRedeemDO;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderRedeemService;
import com.snb.deal.task.OrderRedeemAsyncTask;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.*;
import com.snb.third.yifeng.dto.order.redeem.RedeemPortfolioRequest;
import com.snb.third.yifeng.dto.order.redeem.RedeemPortfolioResponse;
import com.snb.third.yifeng.enums.YfTransactionTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/4/12 21:01
 */

@Service
@Slf4j
public class OrderRedeemBizImp implements OrderRedeemBiz {

    @Resource
    private OrderRedeemService orderRedeemService;

    @Resource
    private OrderRedeemAsyncTask orderRedeemAsyncTask;

    @Resource
    private FundPortfolioService fundPortfolioService;

    @Resource
    private FlowNumberService flowNumberService;

    @Resource
    private Environment environment;

    @Event(reliability = true,
            eventType = "'redeemApply'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.redeem.apply",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public OrderRedeemResponse orderRedeem(OrderRedeemBO orderRedeemBO) throws Exception {
        log.info("创建赎回订单开始-orderRedeemRequest-{}", orderRedeemBO.toString());
        //1.创建赎回订单
        OrderRedeemDO orderRedeemDO = orderRedeemService.createOrderRedeem(orderRedeemBO);
        //2.发送赎回请求
        String notifyUrl = environment.getProperty("snb.api.url.domain") + "/" + environment.getProperty("ifast.order.callback.url");
        RedeemPortfolioRequest redeemPortfolioRequest = new RedeemPortfolioRequest();
        redeemPortfolioRequest.setAccountNumber(orderRedeemDO.getAccountNumber());
        redeemPortfolioRequest.setMerchantNumber(orderRedeemDO.getMerchantNumber());
        redeemPortfolioRequest.setPortfolioId(Long.parseLong(orderRedeemDO.getPortfolioId()));
        redeemPortfolioRequest.setRedemptionAmount(orderRedeemDO.getTransactionAmount());
        redeemPortfolioRequest.setInvestorPayId(orderRedeemDO.getInvestorPayId());
        redeemPortfolioRequest.setNotifyUrl(notifyUrl);
        log.info("发送赎回请求开始-redeemPortfolioRequest-{}", redeemPortfolioRequest.toString());
        BaseResponse<RedeemPortfolioResponse> baseResponse = (BaseResponse<RedeemPortfolioResponse>) fundPortfolioService.redeemPortfolio(redeemPortfolioRequest);
        log.info("发送赎回请求结束-baseResponse-{}", baseResponse.toString());
        //3.补偿赎回表，赎回详情表
        OrderRedeemResponse redeemResponse = orderRedeemService.compensateOrderRedeem(baseResponse, orderRedeemDO);
        //4.赎回完成页设置赎回金额、赎回费用、赎回基金交易预计确认日期、到帐日期
        orderRedeemService.orderRedeemComplete(redeemResponse, baseResponse.getData(), orderRedeemDO);
        //5.发送通知
        try {
            OrderRedeemSyncNotify orderRedeemSyncNotify = new OrderRedeemSyncNotify();
            BeanUtils.copyProperties(orderRedeemDO, orderRedeemSyncNotify);
            orderRedeemSyncNotify.setEventId(orderRedeemDO.getUserId() + orderRedeemDO.getOrderNo());
            EventMessageContext.addMessage(orderRedeemSyncNotify);
        } catch (Exception e) {
            log.error("orderRedeem-exchange.orderRedeem.redeemNotify is error,orderRedeemBO:{}", orderRedeemBO.toString(), e);
        }
        return redeemResponse;
    }

    @Override
    public void syncOrderRedeem() throws InterruptedException {
        //1.查询异步同步赎回订单请求集合
        log.info("查询异步同步赎回订单开始");
        List<OrderRedeemAsyncBO> orderRedeemAsyncBOList = orderRedeemService.queryOrderRedeemAsync(0,0);
        if (CollectionUtils.isEmpty(orderRedeemAsyncBOList)) {
            log.info("查询异步同步赎回订单结束-查询记录为空");
            return;
        }
        log.info("查询异步同步赎回订单执行开始，数量：{}", orderRedeemAsyncBOList.size());
        final CountDownLatch latch = new CountDownLatch(orderRedeemAsyncBOList.size());
        for (OrderRedeemAsyncBO orderRedeemAsyncBO : orderRedeemAsyncBOList) {
            orderRedeemAsyncTask.doRedeemTask(orderRedeemAsyncBO, latch);
        }
        latch.await();
        log.info("查询异步同步赎回订单结束");
    }

    @Override
    public void syncOrderRedeemCallBack(String merchantNumber, Integer channel) throws Exception {
        List<OrderRedeemDO> orderRedeemDOList = orderRedeemService.queryByMerchantNumber(merchantNumber, channel);
        if (CollectionUtils.isEmpty(orderRedeemDOList)) {
            log.error("syncOrderRedeemCallBack queryByMerchantNumber is null,merchantNumber:{},channel:{}", merchantNumber, channel);
            return;
        }
        for (OrderRedeemDO orderRedeemDO : orderRedeemDOList) {
            //发送查询订单请求
            SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRedeemDO.getAccountNumber(), orderRedeemDO.getMerchantNumber());
            log.info("syncOrderRedeemCallBack-request,发送查询订单请求:{}", syncOrderRequest.toString());
            BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>) fundPortfolioService.syncOrder(syncOrderRequest);
            log.info("syncOrderRedeemCallBack-result,发送查询订单响应:{}", baseResponse.toString());
            OrderRedeemAsyncBO orderRedeemAsyncBO = new OrderRedeemAsyncBO();
            BeanUtils.copyProperties(orderRedeemDO, orderRedeemAsyncBO);
            orderRedeemService.syncOrderRedeem(orderRedeemAsyncBO, baseResponse);
        }
    }

    @Override
    public void syncForceOrderRedeem(String userId, String accountNumber) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(accountNumber)) {
            log.info("同步强制赎回订单,参数为空,userId:{},accountNumber:{}", userId, accountNumber);
            return;
        }
        log.info("同步强制赎回订单,userId:{},accountNumber:{}", userId, accountNumber);
        String date = DateTimeUtil.getCurrentDatetime(DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_LINE);
        YfQueryOrderListRequest yfQueryOrderListRequest = YfQueryOrderListRequest.builder()
                .accountNumber(accountNumber)
                .transactionTypes(YfTransactionTypeEnum.FORCE_SELL.getType())
                .pricedDateStart(date)
                .pricedDateEnd(date)
                .build();
        log.info("同步用户：{}发起强制赎回订单请求：{}", userId, yfQueryOrderListRequest);
        BaseResponse<YfQueryOrderListResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfQueryOrderListResponse>) fundPortfolioService.querySpecialOrderList(yfQueryOrderListRequest);
        } catch (Exception e) {
            baseResponse = null;
            log.error("syncForceOrderRedeem-querySpecialOrderList is error,userId:{},accountNumber:{}", userId, accountNumber, e);
        }
        if (baseResponse == null || !baseResponse.success() || baseResponse.getData() == null || CollectionUtils.isEmpty(baseResponse.getData().getData())) {
            log.info("同步用户：{}发起强制赎回查询结果为空：{}", userId, yfQueryOrderListRequest);
            return;
        }
        log.info("同步用户：{}发起强制赎回订单响应：{}", userId, baseResponse.toString());
        List<YfOrderInfo> orderInfoList = baseResponse.getData().getData();
        for (YfOrderInfo yfOrderInfo : orderInfoList) {
            if (yfOrderInfo == null) {
                continue;
            }
            this.createForceOrderRedeem(yfOrderInfo, userId);
        }
    }

    private void createForceOrderRedeem(YfOrderInfo yfOrderInfo, String userId) {
        try {
            ForceOrderBO forceOrderBO = new ForceOrderBO();
            BeanUtils.copyProperties(yfOrderInfo, forceOrderBO);
            forceOrderBO.setUserId(userId);
            forceOrderBO.setPortfolioCode(StringUtils.EMPTY);
            orderRedeemService.createForceOrderRedeem(forceOrderBO);
        } catch (Exception e) {
            log.error("createForceOrderRedeem is error:{}", yfOrderInfo.toString(), e);
        }
    }
}
