package com.snb.deal.mapper.order;

import com.snb.deal.entity.order.OrderDividendDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDividendMapper {

    int insert(OrderDividendDO record);

    OrderDividendDO queryByOrderNo(OrderDividendDO record);

    List<OrderDividendDO> queryByMerchantNumber(@Param("merchantNumber") String merchantNumber, @Param("channel") Integer channel);

}