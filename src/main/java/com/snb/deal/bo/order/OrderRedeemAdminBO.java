package com.snb.deal.bo.order;

import com.snb.deal.entity.order.OrderRedeemDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * 为后台赎回订单列表页面准备的实体
 *
 * @author yunpeng.zhang
 * @date 2018/4/13 21:46
 */
@Data @EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class OrderRedeemAdminBO extends OrderRedeemDO {
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
    private Date transactionFinishTime;

}
