package com.snb.deal.mapper.order;


import com.snb.deal.bo.rebalance.OrderRebalanceAuthConditionBO;
import com.snb.deal.entity.order.OrderRebalanceAuthDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRebalanceAuthMapper {

    /**
     * 新增
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    int insert(OrderRebalanceAuthDO orderRebalanceAuthDO);

    /**
     * 更新授权信息
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    Long update(OrderRebalanceAuthDO orderRebalanceAuthDO);

    /**
     * 查询用户授权记录数目
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    Long queryOrderRebalanceAuthCount(OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO);

    /**
     * 查询用户授权记录数目
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    OrderRebalanceAuthDO queryOrderRebalanceAuth(OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO);

    /**
     * 用户当日未授权的调仓数量
     * @author RunFa.Zhou
     * @date 2018-05-05
     * @return
     */
    Long currentDayUnAuthCount(OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO);

    OrderRebalanceAuthDO selectLastUnProcessByUserId(String userId);
}