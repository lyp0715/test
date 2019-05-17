package com.snb.deal.task;


import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.jianlc.schedule.service.IScheduleExecuteRecordService;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

@Component
@Slf4j
public class FundRebalanceSendTask extends ScheduleTaskDealAbstract {
    @Autowired
    IScheduleExecuteRecordService scheduleExecuteRecordService;
    @Resource
    private FundRebalanceBiz fundRebalanceBiz;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {
        long startTime = System.currentTimeMillis();

        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        boolean rst = true;
        String rstMsg = "调仓定时任务处理成功！";

        try {
            fundRebalanceBiz.sendRebalance();
        } catch (Exception e) {
            log.error("调仓定时任务处理失败！", e);
            rstMsg = "调仓定时任务处理失败！";
            rst = false;
        }

        saveScheduleResult(scheduleExecuteRecord, scheduleServer, startTime, rst, rstMsg);
        log.info("调仓定时任务处理耗时：{}", System.currentTimeMillis() - startTime);
        traceContextOffSpan();
        return true;
    }

}
