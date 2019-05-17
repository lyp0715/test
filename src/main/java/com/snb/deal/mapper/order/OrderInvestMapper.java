package com.snb.deal.mapper.order;

import com.snb.deal.admin.api.dto.order.OrderInvestListRequest;
import com.snb.deal.api.dto.plan.PlanAutoInvestRequest;
import com.snb.deal.bo.order.OrderInvestAdminBO;
import com.snb.deal.bo.order.OrderInvestBO;
import com.snb.deal.entity.order.OrderInvestDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInvestMapper {

    int insert(OrderInvestDO record);

    /**
     * 根据平台手机号、商户交易流水号查询买入订单信息 （为后台准备的查询）
     * @param request 包含平台手机号和用户姓名
     * @return
     * @author yunpeng.zhang
     */
    List<OrderInvestAdminBO> listByOrderInvestListCondition(OrderInvestListRequest request);

    int updateOrderInvest(OrderInvestDO orderInvestDO);

    List<OrderInvestDO> queryByOrderNo(OrderInvestDO record);

    List<OrderInvestBO> querySyncOrderList(@Param("businessCode") String[] businessCode, @Param("orderStatus") Integer orderStatus,
                                           @Param("channel") Integer channel);
    OrderInvestDO getByMerchantNumberAndChannel(@Param("merchantNumber") String merchantNumber, @Param("channel") Integer channel);

    OrderInvestDO getByOrderInvestId(@Param("orderInvestId") Long orderInvestId);

    Long count(@Param("userId") String userId,@Param("investType") int investType);


    /**
     * 根据条件查询计划里的定投记录详情
     *
     * @param request
     * @return
     */
    List<OrderInvestDO> listByAutoInvestCondition(PlanAutoInvestRequest request);

}