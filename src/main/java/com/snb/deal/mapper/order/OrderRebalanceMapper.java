package com.snb.deal.mapper.order;

import com.snb.deal.admin.api.dto.order.OrderRebalanceListRequest;
import com.snb.deal.bo.order.OrderRebalanceAdminBO;
import com.snb.deal.bo.rebalance.OrderRebalanceAsyncBO;
import com.snb.deal.bo.rebalance.OrderRebalanceConditionBO;
import com.snb.deal.bo.rebalance.OrderRebalanceSendBO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderRebalanceMapper {


    /**
     * 根据平台手机号、商户交易流水号查询调仓信息 （为后台准备的查询）
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    List<OrderRebalanceAdminBO> listByOrderRebalanceCondition(OrderRebalanceListRequest request);


    /**
     * 查询待同步的调仓订单列表
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    List<OrderRebalanceAsyncBO> queryOrderRebalanceAsync(@Param("channel") Integer channel,@Param("businessCode")Integer businessCode,
                                                         @Param("transactionStatus")Integer transactionStatus,
                                                         @Param("pagetIndex")Integer pagetIndex,@Param("pageSize")Integer pageSize);

    /**
     * 通过调仓订单号，更新调仓订单状态
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    int updateOrderRebalanceBycondition(OrderRebalanceDO orderRebalanceDO) throws Exception;


    /**
     * 通过条件查询调仓订单数量
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    Long queryOrderRebalanceCountBycondition(OrderRebalanceConditionBO orderRebalanceConditionBO);

    /**
     * 创建调仓订单信息
     * @author RunFa.Zhou
     * @date 2018-04-17
     * @return
     */
    int createOrderRebalanceInfo(OrderRebalanceDO orderRebalanceDO) throws Exception;


    /**
     * @Description 根据订单号查找
     * @author lizengqiang
     * @date 2018/4/18 11:34
     * @param orderRebalanceDO
     * @return 
     */
    OrderRebalanceDO queryByOrderNo(OrderRebalanceDO orderRebalanceDO);


    /**
     * 通过条件查询调仓对象信息
     * @author RunFa.Zhou
     * @date 2018-05-05
     * @return
     */
    OrderRebalanceDO queryOrderRebalanceDOByCondition(OrderRebalanceConditionBO orderRebalanceConditionBO);

    /**
     * 查询需要发送的调仓订单
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    List<OrderRebalanceSendBO> querySendOrderRebalanceList(@Param("authStatus") Integer authStatus,
                                                           @Param("startIndex") int startIndex,@Param("count") int count);

    /**
     * @author yunpeng.zhang
     */
    OrderRebalanceDO selectByOrderRebalanceId(Long orderRebalanceId);

    /**
     * 查询待修复的调仓订单列表
     * @param channel
     * @param businessCode
     * @param pagetIndex
     * @param pageSize
     * @return
     */
    List<OrderRebalanceAsyncBO> queryOrderRebalanceRepaired(@Param("channel") Integer channel,@Param("businessCode")Integer businessCode,
                                                         @Param("pagetIndex")Integer pagetIndex,@Param("pageSize")Integer pageSize);
}