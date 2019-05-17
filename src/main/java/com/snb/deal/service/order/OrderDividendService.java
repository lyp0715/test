package com.snb.deal.service.order;

import com.snb.deal.bo.order.OrderDividendBO;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderDividendDO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.third.yifeng.dto.order.YfQueryOrderListResponse;

import java.util.List;

/**
 * 红利再投订单
 */
public interface OrderDividendService {

    /**
     * 保存红利再投订单
     * @param orderDividendBO
     * @param yfQueryOrderListResponse
     * @return
     * @throws Exception
     */
    void createDividendOrder(OrderDividendBO orderDividendBO, YfQueryOrderListResponse yfQueryOrderListResponse) throws Exception;

    /**
     * @param orderInfoDO
     * @param orderListBO
     * @return
     * @Description 订单列表获取分红
     * @author lizengqiang
     * @date 2018/4/18 20:27
     */
    OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO);

}
