package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service("planInfoSyncTask")
public class PlanInfoSyncTask  extends ScheduleTaskDealAbstract{

    @Resource
    private PlanService planService;
    @Resource
    private PlanBiz planBiz;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        Integer successNum=0,failedNum=0;
        Boolean result = Boolean.TRUE;
        String resultMsg = "更新计划状态%s个，失败%s个";
        try {
            Integer pageNo=0,pageSize=1000;
            List<PlanInfoDO> planInfoDOList = planService.pagePlanInfo(pageNo,pageSize);
            while (CollectionUtils.isNotEmpty(planInfoDOList)) {

                for (PlanInfoDO planInfoDO : planInfoDOList) {

                    Long planInfoId = planInfoDO.getPlanInfoId();
                    String userId = planInfoDO.getUserId();

                    try {
                        int num = planBiz.syncPlanInfo(planInfoDO);
                        if (num == 1) {
                            successNum++;
                        }
                    } catch (Exception e) {
                        log.error("用户：{}定投计划：{}执行同步异常",userId,planInfoId,e);
                        failedNum++;
                    }
                }

                pageNo++;
                planInfoDOList = planService.pagePlanInfo(pageNo,pageSize);
            }


        } catch (Exception e) {
            log.error("定投计划同步执行异常",e);
            result = Boolean.FALSE;
            resultMsg="任务执行异常";
        }
        resultMsg = String.format(resultMsg,successNum,failedNum);
        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
