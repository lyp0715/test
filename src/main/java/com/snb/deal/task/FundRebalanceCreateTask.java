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


/**
 * 调仓业务-调仓任务创建
 *
 * @author RunFa.Zhou
 * @date 2018-04-28
 * @return
 */
@Component
@Slf4j
public class FundRebalanceCreateTask extends ScheduleTaskDealAbstract {
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
            fundRebalanceBiz.createRebalance();
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
