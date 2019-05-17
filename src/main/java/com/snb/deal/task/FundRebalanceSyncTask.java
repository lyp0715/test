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
public class FundRebalanceSyncTask extends ScheduleTaskDealAbstract {
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
        String rstMsg = "调订单同步任务处理成功！";

        try {
            boolean isAllSuccess = fundRebalanceBiz.rebalanceOrderSync();
            if (!isAllSuccess) {
                rstMsg = "调订单同步任务处理存在问题，请查看系统日志！";
            }
        } catch (Exception e) {
            log.error("调订单同步任务处理失败！", e);
            rstMsg = "调订单同步任务处理存在问题，请查看系统日志！";
            rst = false;
        }

        saveScheduleResult(scheduleExecuteRecord, scheduleServer, startTime, rst, rstMsg);
        log.info("调订单同步任务处理耗时：{}", System.currentTimeMillis() - startTime);
        traceContextOffSpan();
        return true;
    }


}
