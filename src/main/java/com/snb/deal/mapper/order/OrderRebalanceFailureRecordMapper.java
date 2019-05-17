package com.snb.deal.mapper.order;


import com.snb.deal.entity.order.OrderRebalanceFailureRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRebalanceFailureRecordMapper {

    /**
     * 新增调仓失败记录
     * @author RunFa.Zhou
     * @date 2018-04-24
     * @return
     */
    int insert(OrderRebalanceFailureRecordDO record);
}
