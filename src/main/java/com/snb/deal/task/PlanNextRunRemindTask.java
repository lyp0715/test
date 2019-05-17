package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 计划下次执行提醒（目前仅有短信）
 */
@Slf4j
@Component
public class PlanNextRunRemindTask extends ScheduleTaskDealAbstract{

    @Resource
    private PlanService planService;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        Integer successNum=0,failedNum=0;
        Boolean result = Boolean.TRUE;
        String tommorrow = DateTime.now().plusDays(1).toString("yyyy-MM-dd");
        Date tommorrowDate = new DateTime(tommorrow).toDate();
        String resultMsg = "%s执行的计划，成功提醒%s个，失败%s个";
        try {
            Integer pageNo=0,pageSize=1000;
            //分页查询明天执行的计划
            log.info("查询：{}执行的定投计划",tommorrow);
            List<PlanInfoDO> planInfoDOList = planService.pageAvailablePlanByRunDate(tommorrowDate,pageNo,pageSize);
            while (CollectionUtils.isNotEmpty(planInfoDOList)) {

                for (PlanInfoDO planInfoDO : planInfoDOList) {

                    Long planInfoId = planInfoDO.getPlanInfoId();
                    String userId = planInfoDO.getUserId();

                    try {
                        planService.planNextRunRemind(planInfoDO);
                        successNum++;
                    } catch (Exception e) {
                        log.error("用户：{}定投计划：{}执行提醒异常",userId,planInfoId,e);
                        failedNum++;
                    }
                }

                pageNo++;
                planInfoDOList = planService.pageAvailablePlanByRunDate(tommorrowDate,pageNo,pageSize);
            }


        } catch (Exception e) {
            log.error("定投计划执行提醒任务执行异常",e);
            result = Boolean.FALSE;
            resultMsg="任务执行异常";
        }
        resultMsg = String.format(resultMsg,tommorrow,successNum,failedNum);
        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
