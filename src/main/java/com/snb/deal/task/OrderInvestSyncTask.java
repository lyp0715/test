package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.deal.biz.invest.InvestBiz;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 投资订单同步
 */
@Service("orderInvestAsyncTask")
@Slf4j
public class OrderInvestSyncTask extends ScheduleTaskDealAbstract{
    @Resource
    InvestBiz investBiz;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        boolean result = Boolean.TRUE;
        String resultMsg = "投资订单同步完成";
        try {

            log.info("投资订单同步任务开始执行");
            investBiz.syncInvestOrder();

        } catch (Exception e) {
            log.error("投资订单同步任务异常",e);
            result = Boolean.FALSE;
            resultMsg = "投资订单同步异常";
        }

        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
