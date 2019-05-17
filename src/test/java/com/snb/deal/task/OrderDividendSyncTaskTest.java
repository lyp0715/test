package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.snb.BaseBeanTest;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class OrderDividendSyncTaskTest extends BaseBeanTest {

    @Resource
    private PlanService planService;
    @Resource
    private PlanBiz planBiz;

    @Test
    public void execute() {
//        long startTime = System.currentTimeMillis();
//        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
//        scheduleExecuteRecord.setExecuteTime(new Date());
//        Integer successNum=0,failedNum=0;
//        Boolean result = Boolean.TRUE;
//        String resultMsg = "同步定投计划执行记录，成功%s个，失败%s个";
        try {
            //查询当天执行的定投计划
            Date today = new DateTime(DateTime.now().toString("yyyy-MM-dd")).toDate();
            Integer pageNo=0,pageSize=1000;
            log.info("开始同步：{}执行的定投计划的执行记录",today);
            List<PlanInfoDO> planInfoDOList = planService.pageAvailablePlanByRunDate(today,pageNo,pageSize);
            while (CollectionUtils.isNotEmpty(planInfoDOList)) {

                for (PlanInfoDO planInfoDO : planInfoDOList) {
                    Long planInfoId = planInfoDO.getPlanInfoId();
                    String userId = planInfoDO.getUserId();
                    try {
                        planBiz.syncPlanExecuteRecord(planInfoDO);
//                        successNum++;
                    } catch (Exception e) {
                        log.error("用户：{}定投计划：{}同步执行记录异常",userId,planInfoId,e);
//                        failedNum++;
                    }
                }
                pageNo++;
                planInfoDOList = planService.pageAvailablePlanByRunDate(today,pageNo,pageSize);
            }

        } catch (Exception e) {
            log.error("同步定投计划执行记录",e);
//            result = Boolean.FALSE;
//            resultMsg="任务执行异常";
        }
//        resultMsg = String.format(resultMsg,successNum,failedNum);
    }
}