package com.snb.deal.biz.invest.impl;

import com.snb.BaseBeanTest;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.invest.AutoInvestRequest;
import com.snb.deal.api.dto.invest.InvestRequest;
import com.snb.deal.biz.invest.InvestBiz;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

public class InvestBizImplTest extends BaseBeanTest{
    @Resource
    InvestBiz investBiz;

    @Test
    public void invest() throws Exception {

        InvestRequest request = new InvestRequest();
        request.setUserId("f6f82b76017442cb838665dd6c8b8d74");
        request.setPlanId(48722657042432L);
        request.setInvestorPayId("11380");
        request.setInvestAmount(new BigDecimal(40000));
        request.setFundUserAccount("JLC20180518000028029");
        request.setFundUserAccountId(1);
        request.setChannel(FundChannelEnum.YIFENG);

        investBiz.invest(request);
    }

    @Test
    public void syncInvestOrder() throws Exception {

        investBiz.syncInvestOrder();
    }

    @Test
    public void getBuyTransactionAndExpectedConfirmedDate() throws Exception {
    }

    @Test
    public void getOrderInvestFee() throws Exception {
    }

    @Test
    public void autoInvest() throws Exception {

        AutoInvestRequest request = new AutoInvestRequest();
        request.setThirdPlanId("23801");
        request.setMerchantNumber("JLC20180508000038305");
        request.setChannel(FundChannelEnum.YIFENG);

        investBiz.autoInvest(request);

    }

    @Test
    public void syncInvestOrderByMechantNumber() throws Exception {

        investBiz.syncInvestOrder("JLC20180503es5bz",FundChannelEnum.YIFENG);
    }

}