package com.snb.deal.task;


import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import com.snb.deal.bo.order.OrderRedeemAsyncBO;
import com.snb.deal.service.order.OrderRedeemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class FundRedeemSyncTask extends ScheduleTaskDealAbstract {


    @Resource
    private OrderRedeemBiz orderRedeemBiz;
    @Autowired
    private Environment environment;
    @Resource
    private OrderRedeemService orderRedeemService;


    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        log.info("基金赎回同步任务处理开始：startTime:{}", startTime);
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        boolean rst = true;
        String rstMsg = "共同步赎回订单%s";
        Integer pageNo=0,pageSize = environment.getProperty("redeem.syncRedeemOrder.queryCount", Integer.class, 500),total=0;
        try {
            log.info("同步赎回订单开始");
            List<OrderRedeemAsyncBO> orderRedeemAsyncBOList = orderRedeemService.queryOrderRedeemAsync(pageNo,pageSize);
            while (CollectionUtils.isNotEmpty(orderRedeemAsyncBOList)) {

                for (OrderRedeemAsyncBO orderRedeemAsyncBO : orderRedeemAsyncBOList) {
                    try {
                        orderRedeemService.syncOrderRedeem(orderRedeemAsyncBO);
                        total++;
                    } catch (Exception e) {
                        log.error("同步赎回订单：{}异常",orderRedeemAsyncBO.getMerchantNumber(),e);
                    }
                }
                pageNo++;

                orderRedeemAsyncBOList = orderRedeemService.queryOrderRedeemAsync(pageNo,pageSize);
            }
            rstMsg=String.format(rstMsg,total);
        }catch (Exception e){
            log.error("基金赎回同步任务处理失败！",e);
            rstMsg = "基金赎回同步任务处理失败！";
        }
        saveScheduleResult(scheduleExecuteRecord, scheduleServer, startTime, rst, rstMsg);
        log.info("基金赎回同步任务处理耗时：{}", System.currentTimeMillis() - startTime);
        traceContextOffSpan();
        return true;
    }
}
