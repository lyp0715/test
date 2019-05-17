package com.snb.deal.bo.order;

import com.snb.deal.entity.order.OrderInvestDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

/**
 * 为后台买入订单列表页面准备的实体
 */
@Data @EqualsAndHashCode(callSuper = true)
@ToString
public class OrderInvestAdminBO extends OrderInvestDO {
    /**
     * 平台手机号
     */
    private String phone;

    /**
     * 用户姓名
     */
    private String name;
    /**
     * 确认时间
     */
    private Date pricedDate;
    /**
     * 下单日期
     */
    private Date orderDate;

}
