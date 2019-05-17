package com.snb.deal.entity.plan;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 计划执行记录
 */
@Data
@ToString
public class PlanExecuteRecordDO extends BaseBean{

    private Long planExecuteRecordId;

    private String userId;

    private Long planInfoId;

    private String thirdPlanId;

    private String accountNumber;

    private String merchantNumber;

    private Date executeTime;

}