package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 定投计划信息
 */
@Data
@ToString
public class PlanInfoDO extends BaseBean {
    /**
     * 计划id
     */
    private Long planInfoId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 第三方基金账户id
     */
    private Long fundUserAccountId;
    /**
     * 第三方计划id
     */
    private String thirdPlanId;
    /**
     * 渠道 1.奕丰
     */
    private Integer channel;

    /**
     * 计划名称
     */
    private String planName;
    /**
     * 定投周期:0:每周1:每两周2:每月3:每季
     */
    private Integer cycle;
    /**
     * 定投日期
     */
    private Integer cycleDay;
    /**
     * 是否立即扣款1:表示立即扣款2:否
     */
    private Integer implement;
    /**
     * 失败时是否顺延扣款1：顺延0：不顺延
     */
    private Integer postponeDeduction;
    /**
     * 每月定投金额
     */
    private BigDecimal portfolioAmount;
    /**
     * 目标金额
     */
    private BigDecimal targetAmount;
    /**
     * 定投年限(年)
     */
    private Integer portfolioYear;
    /**
     * 1:正常2：暂停3：终止
     */
    private Integer planStatus;
    /**
     * 计划执行日期
     */
    private Date nextRunDate;

}