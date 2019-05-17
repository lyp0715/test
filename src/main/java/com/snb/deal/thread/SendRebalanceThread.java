package com.snb.deal.thread;

import com.alibaba.fastjson.JSONObject;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.rebalance.OrderRebalanceSendBO;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class SendRebalanceThread implements Callable<Boolean> {
    private FundRebalanceBiz fundRebalanceBiz;
    private OrderRebalanceSendBO orderRebalanceSendBO;

    public SendRebalanceThread(OrderRebalanceSendBO orderRebalanceSendBO,FundRebalanceBiz fundRebalanceBiz) {
        this.orderRebalanceSendBO = orderRebalanceSendBO;
        this.fundRebalanceBiz = fundRebalanceBiz;
    }

    @Override
    public Boolean call() {
        try {
            fundRebalanceBiz.singleSendRebalance(orderRebalanceSendBO);
            return true;
        } catch (Exception e) {
            log.error("{} 调仓任务:{}失败", Thread.currentThread(),JSONObject.toJSONString(orderRebalanceSendBO), e);
            return false;
        }
    }
}
