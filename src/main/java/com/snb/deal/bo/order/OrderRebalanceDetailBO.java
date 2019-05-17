package com.snb.deal.bo.order;

import com.snb.deal.entity.order.OrderRebalanceDetailDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true)
@ToString
public class OrderRebalanceDetailBO extends OrderRebalanceDetailDO {
    /**
     * 交易状态
     */
    private String transactionStatusResult;
}
