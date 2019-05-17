package com.snb.deal.task;

import com.google.common.collect.Lists;
import com.jianlc.event.enums.EventStatus;
import com.jianlc.event.retry.EventRetry;
import com.jianlc.event.retry.RetryStatistics;
import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("dealEventSendRetryTask")
@Slf4j
public class DealEventSendRetryTask extends ScheduleTaskDealAbstract{

    @Autowired
    EventRetry defaultSendRetryExecuotr;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());
        long startTime = System.currentTimeMillis();
        Integer successNum = 0,failedNum=0;
        Integer pageSize = 1000;
        boolean result = Boolean.TRUE;
        String resultMsg = "";
        try {
            List<EventStatus> eventStatusList = Lists.newArrayList();
            eventStatusList.add(EventStatus.INIT);
            eventStatusList.add(EventStatus.FAIL);

            RetryStatistics statistics = defaultSendRetryExecuotr.retry(pageSize,eventStatusList);
            successNum+=statistics.getSuccessCount();
            failedNum+=statistics.getFailCount();

            resultMsg=String.format("重试成功:%s条，失败:%s条",successNum,failedNum);
        } catch (Exception e) {
            log.error("失败消息重试任务执行异常",e);
            result = Boolean.FALSE;
            resultMsg = "任务执行异常";
        }
        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
