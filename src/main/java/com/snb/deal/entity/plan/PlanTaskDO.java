package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 定投计划执行任务
 */
@Data
@ToString
public class PlanTaskDO extends BaseBean{
    /**
     * 业务id
     */
    private Long planTaskId;
    /**
     * 计划id
     */
    private Long planInfoId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 计划执行日期
     */
    private Date executeDate;

    /**
     * 1:待执行2：成功3：失败3：处理中4：暂停5：终止
     */
    private Integer taskStatus;

}