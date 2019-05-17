package com.snb.deal.mq;

import com.snb.BaseBeanTest;
import com.snb.common.mq.bean.AutoInvestCompleteMessage;
import org.junit.Test;

import javax.annotation.Resource;

public class PlanPortfolioIdComsumerTest extends BaseBeanTest{

    @Resource
    PlanPortfolioIdComsumer planPortfolioIdComsumer;

    @Test
    public void handleData() throws Exception {


        AutoInvestCompleteMessage message = new AutoInvestCompleteMessage();
        message.setUserId("bf49bc0167c74067872405a9ff4cd398");
        message.setPlanInfoId(40920207380672L);
        message.setThirdPortfolioId("123456");

        planPortfolioIdComsumer.handleData(message);

    }

}