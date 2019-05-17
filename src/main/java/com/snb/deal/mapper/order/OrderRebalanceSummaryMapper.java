package com.snb.deal.mapper.order;


import com.snb.deal.bo.rebalance.OrderRebalanceSummaryConditionBO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface OrderRebalanceSummaryMapper {

    /**
     * 新增
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    int insert(OrderRebalanceSummaryDO orderRebalanceSummaryDO);


    OrderRebalanceSummaryDO queryByCondition(OrderRebalanceSummaryConditionBO orscBO);

    OrderRebalanceSummaryDO getLastRebalanceSummaryByCondition(OrderRebalanceSummaryConditionBO orscBO);

}