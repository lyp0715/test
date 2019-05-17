package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.jianlc.schedule.service.IScheduleExecuteRecordService;
import com.jianlc.schedule.service.IScheduleTaskDeal;
import com.jianlc.tc.jtracker.client.TraceContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/4/16 14:59
 */
public abstract class ScheduleTaskDealAbstract implements IScheduleTaskDeal {
    
    @Autowired
    public IScheduleExecuteRecordService scheduleExecuteRecordService;

    /**
     * 保存定时任务处理结果
     *
     * @param scheduleExecuteRecord
     * @param scheduleServer
     * @param startTime
     * @param rst
     * @param rstMsg
     * @return
     */
    public boolean saveScheduleResult(ScheduleExecuteRecord scheduleExecuteRecord, ScheduleServer scheduleServer,
                                      long startTime, boolean rst, String rstMsg) {
        long endTime = System.currentTimeMillis();
        scheduleExecuteRecord.setConsumingTime(endTime - startTime);
        scheduleExecuteRecord.setNextExecuteTime(scheduleServer.getNextRunStartTime());
        scheduleExecuteRecord.setTaskType(scheduleServer.getBaseTaskType());
        if (rst) {
            scheduleExecuteRecord.setStatus((short) 1);
        } else {
            scheduleExecuteRecord.setStatus((short) 2);
        }
        scheduleExecuteRecord.setExecuteResult(rstMsg);
        scheduleExecuteRecord.setHostName(scheduleServer.getHostName());
        scheduleExecuteRecord.setIp(scheduleServer.getIp());
        scheduleExecuteRecordService.addScheduleExecuteRecord(scheduleExecuteRecord);
        return true;
    }

    public void traceContextOffSpan(){
        try {
            TraceContext.offSpan();
        }catch (Exception e){

        }
    }
}
