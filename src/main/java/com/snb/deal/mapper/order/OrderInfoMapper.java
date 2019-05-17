package com.snb.deal.mapper.order;


import com.snb.deal.bo.rebalance.OrderRebalanceConditionBO;
import com.snb.deal.entity.order.OrderInfoDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInfoMapper {

    int insert(OrderInfoDO orderInfoDO);

    List<OrderInfoDO> query(OrderInfoDO orderInfoDO);

    List<OrderInfoDO> queryInvest(OrderInfoDO orderInfoDO);

    int update(OrderInfoDO orderInfoDO);

    int updateOrderStatus(@Param("orderNo") Long orderNo, @Param("orderStatus") Integer orderStatus);

    OrderInfoDO queryByOrderNo(@Param("orderNo") Long orderNo);
    /**
     * 查询用户是调仓订单已发送但未完成的订单
     * @author RunFa.Zhou
     * @date 2018-05-03
     * @return
     */
    Long queryUnSendOrderRebalance(OrderRebalanceConditionBO orderRebalanceConditionBO);

    /**
     * 查询上一个调仓完成的信息
     * @param orderRebalanceConditionBO
     * @return
     */
    OrderInfoDO queryLastRebalanceByCondition(OrderRebalanceConditionBO orderRebalanceConditionBO);
}