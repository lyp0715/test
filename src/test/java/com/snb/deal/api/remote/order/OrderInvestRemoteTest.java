package com.snb.deal.api.remote.order;

import com.snb.BaseBeanTest;
import com.snb.common.dto.APIResponse;
import com.snb.deal.api.dto.invest.OrderInvestExpectedDateResponse;
import com.snb.deal.api.dto.invest.OrderInvestFeeRequest;
import com.snb.deal.api.dto.invest.OrderInvestFeeResponse;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

public class OrderInvestRemoteTest extends BaseBeanTest{

    @Resource
    private OrderInvestRemote orderInvestRemote;

    @Test
    public void singleInvest() {
    }

    @Test
    public void getBuyTransactionAndExpectedConfirmedDate() {
        String fundCodes = "004473,968013";
        APIResponse<OrderInvestExpectedDateResponse> buyTransactionAndExpectedConfirmedDate = orderInvestRemote.getBuyTransactionAndExpectedConfirmedDate(fundCodes);
        if (buyTransactionAndExpectedConfirmedDate.isSuccess()) {
            System.out.println(buyTransactionAndExpectedConfirmedDate);
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getOrderInvestFee() {
        OrderInvestFeeRequest orderInvestFeeRequest = new OrderInvestFeeRequest();
        orderInvestFeeRequest.setPortfolioCode("TEST007");
        orderInvestFeeRequest.setInvestmentAmount(new BigDecimal("100000"));
        APIResponse<OrderInvestFeeResponse> orderInvestFee = orderInvestRemote.getOrderInvestFee(orderInvestFeeRequest);
        if (orderInvestFee.isSuccess()) {
            System.out.println(orderInvestFee);
        } else {
            System.out.println("失败！");
        }
    }
}