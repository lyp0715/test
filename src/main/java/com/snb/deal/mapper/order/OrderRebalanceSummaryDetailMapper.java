package com.snb.deal.mapper.order;


import com.snb.deal.bo.rebalance.OrderRebalanceSummaryConditionBO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderRebalanceSummaryDetailMapper {

    /**
     * 新增
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    int insert(OrderRebalanceSummaryDetailDO orderRebalanceSummaryDetailDO);

    /**
     * 批量新增
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    void batchInsert(List<OrderRebalanceSummaryDetailDO> orderRebalanceSummaryDetailDOList);

    /**
     * 通过条件查询调仓概要明细
     * @author RunFa.Zhou
     * @date 2018-04-20
     * @return
     */
    List<OrderRebalanceSummaryDetailDO> queryOrderRebalanceSummaryDetail(OrderRebalanceSummaryConditionBO orscBO);
}