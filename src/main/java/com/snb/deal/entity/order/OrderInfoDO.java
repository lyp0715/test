package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.enums.OrderBusinessEnum;
import com.snb.deal.enums.OrderSourceEnum;
import com.snb.deal.enums.OrderStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class OrderInfoDO extends BaseBean {

    /**
     * 用户id
     */
    private String userId;
    /**
     * 订单号（唯一）
     */
    private Long orderNo;

    /**
     * 订单状态
     * {@link com.snb.deal.enums.OrderStatusEnum}
     */
    private Integer orderStatus;

    /**
     * 业务编码
     * {@link com.snb.deal.enums.OrderBusinessEnum}
     */
    private Integer businessCode;

    /**
     * 来源
     * {@link com.snb.deal.enums.OrderSourceEnum}
     */
    private String source;

    /**
     * 交易完成时间
     */
    private Date transactionFinishTime;

    /**
     * 交易金额
     */
    private BigDecimal transactionAmount;
    /**
     * 发送时间
     */
    private Date sendTime;

    /**
     * 基金渠道
     * {@link com.snb.common.enums.FundChannelEnum}
     */
    private Integer channel;

    public OrderInfoDO(Long orderNo, Integer orderStatus) {
        this.orderNo = orderNo;
        this.orderStatus = orderStatus;
        this.transactionFinishTime = new Date();
    }

    public static OrderInfoDO build(OrderInfoBuildEnum orderInfoBuild,OrderBusinessEnum orderBusinessEnum) {
        switch (orderInfoBuild) {
            case DEFAULT:
                return new OrderInfoDO();
            case INIT:
                OrderInfoDO orderInfoDO = new OrderInfoDO();
                orderInfoDO.setOrderStatus(OrderStatusEnum.PROCESS.getCode());
                orderInfoDO.setBusinessCode(orderBusinessEnum.getCode());
                orderInfoDO.setSource(OrderSourceEnum.H5.getCode());
                orderInfoDO.setTransactionFinishTime(null);
                orderInfoDO.setChannel(FundChannelEnum.YIFENG.getChannel());
                orderInfoDO.setSendTime(new Date());
                return orderInfoDO;
            case FORCE:
                OrderInfoDO forceOrderInfoDO = new OrderInfoDO();
                forceOrderInfoDO.setOrderStatus(OrderStatusEnum.COMPLETE.getCode());
                forceOrderInfoDO.setBusinessCode(orderBusinessEnum.getCode());
                forceOrderInfoDO.setSource(OrderSourceEnum.H5.getCode());
                forceOrderInfoDO.setTransactionFinishTime(null);
                forceOrderInfoDO.setChannel(FundChannelEnum.YIFENG.getChannel());
                forceOrderInfoDO.setSendTime(new Date());
                forceOrderInfoDO.setTransactionFinishTime(new Date());
                return forceOrderInfoDO;
            default:
                return new OrderInfoDO();
        }
    }

    public enum OrderInfoBuildEnum implements Serializable {
        DEFAULT(0, "创建对象"),
        INIT(1, "初始化创建赎回订单"),
        FORCE(2, "创建强制赎回订单"),;

        private int code;
        private String desc;

        OrderInfoBuildEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

}