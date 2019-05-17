package com.snb.deal.remote.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.snb.BaseBeanTest;
import com.snb.deal.api.dto.order.OrderListRequest;
import com.snb.deal.api.dto.redeem.OrderRedeemRequest;
import com.snb.deal.api.remote.order.OrderInfoRemote;
import com.snb.deal.api.remote.order.OrderRedeemRemote;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * USER:    huangyunxing
 * TIME:    2018-07-13 15:11
 * COMMENT:
 */
public class OrderInfoRemoteTest extends BaseBeanTest {
    @Resource
    private OrderInfoRemote orderInfoRemote;

    @Reference(version = "1.0")
    private OrderRedeemRemote orderRedeemRemote;

    @Test
    public void testOrderList() {
        OrderListRequest param = new OrderListRequest();
        param.setBusinessCode(0);
        param.setPageNo(1);
        param.setPageSize(1000);
        param.setUserId("ab5c08fb193348979675b8ef65657664");
        orderInfoRemote.orderList(param);
    }
    @Test
    public void redeem(){
        OrderRedeemRequest orderRedeemRequest = new OrderRedeemRequest();
        orderRedeemRequest.setAccountNumber("JLC20180723000029180");
        orderRedeemRequest.setInvestorPayId(Integer.parseInt("11906"));
        orderRedeemRequest.setPlanId(72103889125440L);
        orderRedeemRequest.setTransactionAmount(new BigDecimal(4000));
        orderRedeemRequest.setUserId("1c94e0b3198645069059768d043aeea6");
        orderRedeemRemote.orderRedeem(orderRedeemRequest);
    }
}
