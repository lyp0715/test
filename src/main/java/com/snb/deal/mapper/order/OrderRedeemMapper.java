package com.snb.deal.mapper.order;


import com.snb.deal.admin.api.dto.order.OrderRedemptionListRequest;
import com.snb.deal.bo.order.OrderRedeemAdminBO;
import com.snb.deal.entity.order.OrderRedeemDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRedeemMapper {
    /**
     * @param orderRedeemDO
     * @return
     * @Description
     * @author lizengqiang
     * @date 2018/4/28 18:52
     */
    List<OrderRedeemDO> query(OrderRedeemDO orderRedeemDO);

    /**
     * @param orderStatus
     * @param channel
     * @param businessCode
     * @param limit
     * @return
     * @Description 根据状态分页查询赎回订单
     * @author lizengqiang
     * @date 2018/4/28 18:50
     */
    List<OrderRedeemDO> queryOrder(@Param("orderStatus") Integer orderStatus, @Param("channel") Integer channel,
                                   @Param("businessCode") Integer businessCode, @Param("limit") int limit,@Param("end") int end);

    /**
     * @param orderRedeemDO
     * @return
     * @Description 插入
     * @author lizengqiang
     * @date 2018/4/28 18:53
     */
    int insert(OrderRedeemDO orderRedeemDO);

    /**
     * @param orderRedeemDO
     * @return
     * @Description 根据订单号更新
     * @author lizengqiang
     * @date 2018/4/28 18:53
     */
    int updateByOrderNo(OrderRedeemDO orderRedeemDO);

    /**
     * 根据平台手机号和商户交易流水号查询订单赎回信息（额外包含平台手机号、商户交易流水号、最大到账日期），专门为后台准备的查询
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    List<OrderRedeemAdminBO> listByOrderRedeemListCondition(OrderRedemptionListRequest request);

    /**
     * @param orderNo
     * @return
     * @Description 根据订单号查询有效的赎回订单信息
     * @author lizengqiang
     * @date 2018/4/28 18:55
     */
    OrderRedeemDO queryByOrderNo(Long orderNo);

    /**
     * @param limit
     * @return
     * @Description 分页查询赎回订单交易状态为下单确认中的赎回订单信息，目前主要为重试使用。
     * @author lizengqiang
     * @date 2018/4/28 18:55
     */
    List<OrderRedeemDO> queryReceiving(int limit);

    /**
     * @param merchantNumber
     * @param channel
     * @return
     * @Description 根据交易流水号查询
     * @author lizengqiang
     * @date 2018/5/11 17:48
     */
    List<OrderRedeemDO> queryByMerchantNumber(@Param("merchantNumber") String merchantNumber, @Param("channel") Integer channel);
    /**
     * @param merchantNumber
     * @return
     * @Description 根据交易流水号查询
     * @author lizengqiang
     * @date 2018/5/11 17:48
     */
    List<OrderRedeemDO> queryByMerchantNum(@Param("merchantNumber") String merchantNumber, @Param("channel") Integer channel);
}