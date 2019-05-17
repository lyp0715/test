package com.snb.deal.mapper.order;

import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInvestDetailMapper {

    int insert(OrderInvestDetailDO record);

    int update(OrderInvestDetailDO record);

    /**
     * 根据条件查询买入基金明细列表
     *
     * @param condition
     * @return
     */
    List<OrderInvestDetailDO> listByOrderInvestDetailCondition(OrderInvestDetailRequest condition);

    int insertBatch(List<OrderInvestDetailDO> orderInvestDetailDOS);

    OrderInvestDetailDO queryInvestDetail(OrderInvestDetailDO orderInvestDetailDO);
}