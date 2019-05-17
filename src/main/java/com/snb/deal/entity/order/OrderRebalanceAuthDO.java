package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 调仓授权-DO
 * @author RunFa.Zhou
 * @date 2018-04-19
 * @return
 */
@Data
@ToString
@EqualsAndHashCode
public class OrderRebalanceAuthDO extends BaseBean {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 调仓授权记录ID
     */
    private Long orderRebalanceAuthId;

    /**
     * 调仓订单ID
     */
    private Long orderRebalanceId;

    /**
     * 用户Id
     */
    private String userId;

    /**
     * 授权状态：1待授权2已授权
     */
    private Integer authStatus;

    /**
     * 渠道
     */
    private Integer channel;




}