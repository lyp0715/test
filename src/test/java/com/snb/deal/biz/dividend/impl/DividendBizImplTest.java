package com.snb.deal.biz.dividend.impl;

import com.snb.BaseBeanTest;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.order.OrderDividendSyncRequest;
import com.snb.deal.biz.dividend.DividendBiz;
import org.junit.Test;

import javax.annotation.Resource;

public class DividendBizImplTest extends BaseBeanTest{

    @Resource
    DividendBiz dividendBiz;

    @Test
    public void syncDividendOrder() throws Exception {

        OrderDividendSyncRequest request = new OrderDividendSyncRequest();
        request.setUserId("e724bd1c17ae42688d8828e38d03bd11");
        request.setAccountNumber("JLC20180508000027748");
        request.setChannel(FundChannelEnum.YIFENG);

        dividendBiz.syncDividendOrder(request);
    }

}