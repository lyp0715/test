package com.snb.deal.service.impl.order;

import com.snb.BaseBeanTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Date;

public class OrderInvestServiceImplTest extends BaseBeanTest{

    @Resource
    private OrderInvestServiceImpl orderInvestService;

    @Test
    public void pageOrderInvest() {
    }

    @Test
    public void listOrderInvestDetail() {
    }

    @Test
    public void createInvestOrder() {
    }

    @Test
    public void afterInvestFailed() {
    }

    @Test
    public void afterInvestApply() {
    }

    @Test
    public void getOrderList() {
    }

    @Test
    public void querySyncOrderList() {
    }

    @Test
    public void syncInvestOrder() {
    }

    @Test
    public void afterAutoInvest() {
    }

    @Test
    public void getOrderExpectedConfirmDate() {
        Date orderExpectedConfirmDate = orderInvestService.getOrderExpectedConfirmDate(44119066165312L);
        System.out.println(orderExpectedConfirmDate);
    }

    @Test
    public void queryByMerchantNumber() {
    }

    @Test
    public void queryByOrderInvestId() {
    }
}