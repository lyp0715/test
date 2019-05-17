package com.snb.deal.thread;

import com.alibaba.fastjson.JSONObject;
import com.jianlc.tc.jtracker.client.TraceContext;
import com.jianlc.tc.jtracker.common.Span;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.rebalance.OrderRebalanceCreateBO;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

@Slf4j
public class CreateRebalanceThread implements Callable<Boolean> {
    private FundRebalanceBiz fundRebalanceBiz;
    private OrderRebalanceCreateBO rebalanceInfoBO;
    private Span span;
    private Integer retryCount=0;

    public CreateRebalanceThread(OrderRebalanceCreateBO rebalanceInfoBO, FundRebalanceBiz fundRebalanceBiz, Span span) {
        this.rebalanceInfoBO = rebalanceInfoBO;
        this.fundRebalanceBiz = fundRebalanceBiz;
        this.span = span;
    }

    @Override
    public Boolean call() {
        try {
            TraceContext.reciveSpan(span.getTraceId(), span.getSpanId());
            fundRebalanceBiz.singleCreateRebalance(rebalanceInfoBO);
            return true;
        } catch (SocketTimeoutException e) {
            log.error("{} 调仓任务:{}超时", Thread.currentThread(), JSONObject.toJSONString(rebalanceInfoBO), e);
            if (retryCount<5) {
                log.error("调仓任务:{}超时，开始重试第:{}次",JSONObject.toJSONString(rebalanceInfoBO),++retryCount);
                return call();
            } else {
                log.error("调仓任务:{}超时，超过最大重试次数",JSONObject.toJSONString(rebalanceInfoBO));
                return false;
            }
        } catch (Exception e) {
            log.error("{} 调仓任务:{}失败", Thread.currentThread(), JSONObject.toJSONString(rebalanceInfoBO), e);
            return false;
        }
    }
}
