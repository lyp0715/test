package com.snb.deal.entity.insurance;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
@EqualsAndHashCode
public class InsuranceDO extends BaseBean {
    /**
     * 保险业务id
     */
    private Long insuranceId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 保险名称
     */
    private String insuranceName;
    /**
     * 保障金额
     */
    private BigDecimal indemnificationAmount;
    /**
     * 保单号
     */
    private String insuranceNo;
    /**
     * 保障期限
     */
    private String indemnificationTimeLimit;
    /**
     * 被保险人姓名
     */
    private String insuredName;
    /**
     * 被保险人身份证号
     */
    private String insuredIdCard;
    /**
     * 被保险人身手机号
     */
    private String insuredPhone;
    /**
     * 购买金额
     */
    private BigDecimal buyAmount;
    /**
     * 保险状态:1未领取2审核中3保障中
     */
    private Integer insuranceStatus;
    /**
     * 保险生效日期
     */
    private Date effectiveDate;
    /**
     * 保险截止日期
     */
    private Date deadlineDate;
    /**
     * 失败原因
     */
    private String reason;
}