package com.snb.deal.mq;

import com.snb.BaseBeanTest;
import com.snb.common.mq.bean.InvestOrderSyncCompleteMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoInvestSmsConsumerTest extends BaseBeanTest{

    @Autowired
    AutoInvestSmsConsumer autoInvestSmsConsumer;

    @Test
    public void handleData() throws Exception {

        InvestOrderSyncCompleteMessage message = new InvestOrderSyncCompleteMessage();
        message.setInvestType(2);
        message.setMerchantNumber("JLC20180511000038381");
        message.setAccountNumber("JLC20180511000027854");
        message.setOrderInvestId(46237679153216L);
        message.setOrderNo(46237679153220L);
        message.setUserId("8752a1ea6f95482f9e6550390edf1cea");

        autoInvestSmsConsumer.handleData(message);
    }

}