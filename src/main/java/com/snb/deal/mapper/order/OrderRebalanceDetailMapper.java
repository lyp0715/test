package com.snb.deal.mapper.order;

import com.snb.deal.admin.api.dto.order.OrderRebalanceDetailRequest;
import com.snb.deal.bo.order.OrderRebalanceDetailBO;
import com.snb.deal.bo.rebalance.OrderRebalanceConditionBO;
import com.snb.deal.entity.order.OrderRebalanceDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderRebalanceDetailMapper {

    /**
     * 查询调仓订单详情列表-管理后台
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    List<OrderRebalanceDetailBO> listByOrderRebalanceDetailCondition(OrderRebalanceDetailRequest condition);

    /**
     * 批量新增调仓明细信息
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    void batchAddRebalanceDetail(List<OrderRebalanceDetailDO> orderRebalanceDetailList);



    /**
     * 批量新增调仓明细信息
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    void batchAddRebalanceDetailSync(List<OrderRebalanceDetailDO> orderRebalanceDetailList);

    /**
     * 批量更新子订单信息
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    void batchUpdateRebalanceDetail(List<OrderRebalanceDetailDO> orderRebalanceDetailList);

    /**
     * 通过查询条件查询数量
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    Long queryRebalanceCountByCondition(OrderRebalanceConditionBO orderRebalanceConditionBO);

    /**
     * 存在未完成的订单
     * @author RunFa.Zhou
     * @date 2018-04-27
     * @return
     */
    Long existNoFinishOrder(OrderRebalanceConditionBO orderRebalanceConditionBO);
}