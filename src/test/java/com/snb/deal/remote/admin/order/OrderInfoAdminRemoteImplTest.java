package com.snb.deal.remote.admin.order;

import com.github.pagehelper.PageInfo;
import com.snb.BaseBeanTest;
import com.snb.deal.admin.api.dto.order.*;
import com.snb.deal.admin.api.remote.order.OrderInfoAdminRemote;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.List;

public class OrderInfoAdminRemoteImplTest extends BaseBeanTest {

    @Resource
    private OrderInfoAdminRemote orderInfoAdminRemote;

    @Test
    public void pageOrderInvestList() {
        OrderInvestListRequest request = new OrderInvestListRequest();
        request.setPhone("18610434650");
        request.setMerchantNumber("11111111");

        PageInfo<OrderInvestListDTO> orderInvestListDTOPageInfo = orderInfoAdminRemote.pageOrderInvestList(request);
        System.out.println(orderInvestListDTOPageInfo);
    }

    @Test
    public void pageOrderRebalanceList() {
        OrderRebalanceListRequest request = new OrderRebalanceListRequest();
        request.setPhone("18868195110");
//        request.setMerchantNumber("1");
        request.setPage(1);
        request.setPageSize(10);
        PageInfo<OrderRebalanceListDTO> pageInfo = orderInfoAdminRemote.pageOrderRebalanceList(request);
        System.out.println(pageInfo);
    }

    @Test
    public void pageOrderRedemptionList() {
        OrderRedemptionListRequest request = new OrderRedemptionListRequest();
        request.setPhone("18610434650");
        request.setMerchantNumber("1");
        PageInfo<OrderRedemptionListDTO> orderRedemptionListDTOPageInfo = orderInfoAdminRemote.pageOrderRedemptionList(request);
        System.out.println(orderRedemptionListDTOPageInfo);
    }

    @Test
    public void getOrderInvestDetail() {
        OrderInvestDetailRequest request = new OrderInvestDetailRequest();
        request.setOrderInvestId(1L);
        List<OrderInvestDetailDTO> orderInvestDetail = orderInfoAdminRemote.getOrderInvestDetail(request);
        System.out.println(orderInvestDetail.size());
    }

    @Test
    public void getOrderRebalanceDetail() {
        OrderRebalanceDetailRequest request = new OrderRebalanceDetailRequest();
        request.setOrderRebalanceId(44843593789440L);
        List<OrderRebalanceDetailDTO> orderRebalanceDetail = orderInfoAdminRemote.getOrderRebalanceDetail(request);
        System.out.println(orderRebalanceDetail.size());
    }

    @Test
    public void getOrderRedemptionDetailDetail() {
        OrderRedemptionDetailRequest request = new OrderRedemptionDetailRequest();
        request.setOrderRedeemId(44057121116224L);
        List<OrderRedemptionDetailDTO> orderRedemptionDetailDTOList = orderInfoAdminRemote.getOrderRedemptionDetail(request);
        System.out.println(orderRedemptionDetailDTOList.size());
    }
}