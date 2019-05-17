package com.snb.deal.service.order;

import com.snb.deal.api.dto.order.OrderListResponse;
import com.snb.deal.bo.order.OrderFeeRateBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author lizengqiang
 * @Description 订单服务
 * @date 2018/4/10 14:41
 */
public interface OrderInfoService {
    /**
     * @param orderListBO
     * @return
     * @Description 订单列表
     * @author lizengqiang
     * @date 2018/4/23 17:08
     */
    OrderListResponse orderList(OrderListBO orderListBO);

    /**
     * @param fundMainModelDetailRequest
     * @param orderFeeList
     * @return
     * @Description 费率计算公式
     * @author lizengqiang
     * @date 2018/4/23 17:08
     */
    BigDecimal calculateFeeRate(FundMainModelDetailRequest fundMainModelDetailRequest, List<OrderFeeRateBO> orderFeeList);

    /**
     * 根据订单号查询订单
     * @param orderNo
     * @return
     */
    OrderInfoDO queryByOrderNo(Long orderNo);
}
