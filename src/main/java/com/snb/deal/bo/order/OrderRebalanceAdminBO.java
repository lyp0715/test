package com.snb.deal.bo.order;

import com.snb.deal.entity.order.OrderRebalanceDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true)
@ToString
public class OrderRebalanceAdminBO extends OrderRebalanceDO {
    /**
     * 平台手机号
     */
    private String phone;
    /**
     * 用户姓名
     */
    private String name;
    /**
     * 调仓结果
     */
    private String orderRebalanceResult;
}
