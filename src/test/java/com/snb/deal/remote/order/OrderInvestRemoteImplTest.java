package com.snb.deal.remote.order;

import com.snb.BaseBeanTest;
import com.snb.deal.api.dto.invest.OrderInvestFeeRequest;
import com.snb.deal.api.remote.order.OrderInvestRemote;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

public class OrderInvestRemoteImplTest extends BaseBeanTest{

    @Resource
    private OrderInvestRemote orderInvestRemote;

    @Test
    public void singleInvest() {
    }

    @Test
    public void getBuyTransactionAndExpectedConfirmedDate() {
    }

    @Test
    public void getOrderInvestFee() {
        OrderInvestFeeRequest request = new OrderInvestFeeRequest();
        request.setInvestmentAmount(new BigDecimal(10000));
        request.setPortfolioCode("TEST007");
        orderInvestRemote.getOrderInvestFee(request);
    }
}