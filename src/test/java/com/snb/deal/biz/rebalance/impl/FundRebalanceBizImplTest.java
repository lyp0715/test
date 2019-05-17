package com.snb.deal.biz.rebalance.impl;

import com.snb.BaseBeanTest;
import com.snb.deal.bo.rebalance.OrderRebalanceCreateBO;
import com.snb.deal.enums.FlowNumberTypeEnum;
import com.snb.deal.service.flowno.FlowNumberService;
import org.junit.Test;

import javax.annotation.Resource;

public class FundRebalanceBizImplTest extends BaseBeanTest{

    @Resource
    FundRebalanceBizImpl fundRebalanceBiz;
    @Resource
    FlowNumberService flowNumberService;

    @Test
    public void createRebalance() {
        fundRebalanceBiz.createRebalance();
    }

    @Test
    public void sendRebalance() {
        fundRebalanceBiz.sendRebalance();
    }

    @Test
    public void singleSendRebalance() {

    }

    @Test
    public void syncOrderRebalanceBiz() {
    }

    @Test
    public void rebalanceOrderSync() {

        fundRebalanceBiz.rebalanceOrderSync();
    }

    @Test
    public void orderRebalanceAsync() {
    }

    @Test
    public void singleCreateRebalance() throws Exception {
        OrderRebalanceCreateBO rebalanceInfoBO = new OrderRebalanceCreateBO();
        rebalanceInfoBO.setFundMainModelId(43846728163328L);
        rebalanceInfoBO.setUserId("da4ef8035a014063ae7dac0888cbc9a7");
        rebalanceInfoBO.setPlanPortfolioRelId(45896291020800L);
        rebalanceInfoBO.setThirdPortfolioId("987279");
        rebalanceInfoBO.setThirdPortfolioCode("TEST007");
        rebalanceInfoBO.setChannel(1);
        rebalanceInfoBO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
        rebalanceInfoBO.setPlanId(45896289374208L);
        fundRebalanceBiz.singleCreateRebalance(rebalanceInfoBO);
    }
}