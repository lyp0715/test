package com.snb.deal.task;

import com.snb.deal.bo.order.OrderRedeemAsyncBO;
import com.snb.deal.service.order.OrderRedeemService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/4/11 14:00
 */
@Slf4j
@Component
public class OrderRedeemAsyncTask {

    @Resource
    private OrderRedeemService orderRedeemService;

    @Resource
    private FundPortfolioService fundPortfolioService;

    @Async("orderRedeemAsyncPool")
    public void doRedeemTask(OrderRedeemAsyncBO orderRedeemAsyncBO, CountDownLatch latch) {
        log.info("{}:doRedeemTask-start，orderRedeemAsyncBO={}", Thread.currentThread(), orderRedeemAsyncBO.toString());
        try {
            //发送查询订单请求
            SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRedeemAsyncBO.getAccountNumber(), orderRedeemAsyncBO.getMerchantNumber());
            log.info("发送查询订单请求:{}", syncOrderRequest.toString());
            BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>) fundPortfolioService.syncOrder(syncOrderRequest);
            log.info("发送查询订单响应:{}", baseResponse.toString());
            orderRedeemService.syncOrderRedeem(orderRedeemAsyncBO, baseResponse);
        } catch (Exception e) {
            log.error("{}:doRedeemTask is error:orderRedeemAsyncBO={}", Thread.currentThread(), orderRedeemAsyncBO.toString(), e);
        }
        log.info("{}:doRedeemTask-end，orderRedeemAsyncBO={}", Thread.currentThread(), orderRedeemAsyncBO.toString());
        latch.countDown();
    }
}
