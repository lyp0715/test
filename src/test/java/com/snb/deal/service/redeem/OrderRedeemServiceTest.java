package com.snb.deal.service.redeem;

import com.snb.common.dto.APIResponse;
import com.snb.deal.ApplicationMain;
import com.snb.deal.api.dto.order.OrderListRequest;
import com.snb.deal.api.dto.order.OrderListResponse;
import com.snb.deal.api.dto.redeem.OrderRedeemResponse;
import com.snb.deal.api.remote.order.OrderInfoRemote;
import com.snb.deal.api.remote.order.OrderRedeemRemote;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import com.snb.deal.bo.order.OrderRedeemBO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class OrderRedeemServiceTest {

    @Resource
    private OrderRedeemBiz orderRedeemBiz;

    @Resource
    private OrderInfoRemote orderInfoRemote;

    @Resource
    private OrderRedeemRemote orderRedeemRemote;

    @Test
    public void orderRedeem() {
        OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
        orderRedeemBO.setUserId("e724bd1c17ae42688d8828e38d03bd11");
        orderRedeemBO.setAccountNumber("JLC20180330000026962");
        orderRedeemBO.setPortfolioId("986124");
        orderRedeemBO.setPortfolioCode("xxx");
        orderRedeemBO.setTransactionAmount(new BigDecimal("1511.31"));
        try {
            OrderRedeemResponse orderRedeemResponse=orderRedeemBiz.orderRedeem(orderRedeemBO);
            System.out.println(orderRedeemResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void syncOrderRedeem() {
        try {
            orderRedeemBiz.syncOrderRedeem();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void orderList() {
        try {
            OrderListRequest orderListRequest = new OrderListRequest();
            orderListRequest.setUserId("e724bd1c17ae42688d8828e38d03bd11");
            orderListRequest.setPageNo(8);
            orderListRequest.setPageSize(20);
            orderListRequest.setBusinessCode(null);
            APIResponse<OrderListResponse> apiResponse = orderInfoRemote.orderList(orderListRequest);
            OrderListResponse orderListResponse=apiResponse.getData();
            System.out.println(orderListResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void syncOrderRedeemCallBack() {
        try {
            String merchantNumber="JLC20180511wtr6x";
            Integer channel=1;
            orderRedeemBiz.syncOrderRedeemCallBack(merchantNumber,channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void syncForceOrderRedeem() {
        try {
            String userId="JLC20180511wtr6x";
            String accountNumber="JLC20180508000027748";
            orderRedeemBiz.syncForceOrderRedeem(userId,accountNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
