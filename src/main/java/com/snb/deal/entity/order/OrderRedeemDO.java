package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.enums.TransactionStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@ToString(callSuper=true, includeFieldNames=true)
public class OrderRedeemDO extends BaseBean {

    /**
     * 业务主键
     */
    private Long orderRedeemId;

    /**
     * 用户id
     */
    private String userId;
    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 交易状态
     * {@link com.snb.deal.enums.TransactionStatusEnum}
     */
    private Integer transactionStatus;

    /**
     * 交易流水号，命名规则为：商户代码+YYYYMMDD(日期)+5位序列号；保证每个订单每个商户唯一
     */
    private String merchantNumber;

    /**
     * 投资者的奕丰账户。在开户接口有返回给商户。
     */
    private String accountNumber;

    /**
     * 赎回的组合编码
     */
    private String portfolioCode;

    /**
     * 赎回的组合编码
     */
    private String portfolioId;

    /**
     * 赎回款至。0=银行卡，1=现金钱包支付
     */
    private Integer paymentMethod = 0;

    /**
     * 投资者账户绑定的支付代码。当paymentMethod=0时，必须上传对应的支付代码。可通过《获取账户的支付方式》获取
     */
    private Integer investorPayId = 0;

    /**
     * 组合赎回金额，2位小数，单位为元
     */
    private BigDecimal transactionAmount;

    /**
     * 交易费用
     */
    private BigDecimal transactionCharge = BigDecimal.ZERO;
    /**
     * 响应编码
     */
    private String responseCode;
    /**
     * 响应详情
     */
    private String responseMessage;

    /**
     * 基金渠道
     * {@link com.snb.common.enums.FundChannelEnum}
     */
    private Integer channel = FundChannelEnum.YIFENG.getChannel();


    public OrderRedeemDO(Long orderNo){
        this.orderNo=orderNo;
    }
    public static OrderRedeemDO build(OrderRedeemBuildEnum orderRedeemBuild) {
        switch (orderRedeemBuild) {
            case DEFAULT:
                return new OrderRedeemDO();
            case INIT:
                OrderRedeemDO orderRedeemDO = new OrderRedeemDO();
                orderRedeemDO.setTransactionStatus(TransactionStatusEnum.RECEIVING.getCode());
                orderRedeemDO.setResponseCode(StringUtils.EMPTY);
                orderRedeemDO.setResponseMessage(StringUtils.EMPTY);
                return orderRedeemDO;
            case FORCE:
                OrderRedeemDO forceOrderRedeemDO = new OrderRedeemDO();
                forceOrderRedeemDO.setResponseCode(StringUtils.EMPTY);
                forceOrderRedeemDO.setResponseMessage(StringUtils.EMPTY);
                return forceOrderRedeemDO;
            default:
                return new OrderRedeemDO();
        }
    }

    public enum OrderRedeemBuildEnum implements Serializable {
        DEFAULT(0, "创建对象"),
        INIT(1, "初始化创建赎回订单"),
        FORCE(2, "创建强制赎回订单"),;

        private int code;
        private String desc;

        OrderRedeemBuildEnum(int code, String desc) {
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