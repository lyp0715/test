package com.snb.deal.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.order.OrderDividendSyncRequest;
import com.snb.deal.biz.dividend.DividendBiz;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 红利再投订单同步任务
 */
@Service("orderDividendSyncTask")
@Slf4j
public class OrderDividendSyncTask extends ScheduleTaskDealAbstract{

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Resource
    private DividendBiz dividendBiz;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        log.info("基金红利再投同步任务处理开始：startTime:{}", startTime);
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        Integer pageNo=0,pageSize=1000,total=0;
        Boolean result = Boolean.TRUE;
        String resultMsg = "";
        try {
            //1. 查询已经开户用户列表
            APIResponse<List<GetUserFundAccountInfoResponse>> apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo,pageSize,FundChannelEnum.YIFENG);
            while(apiResponse.isSuccess() && CollectionUtils.isNotEmpty(apiResponse.getData())) {
                for (GetUserFundAccountInfoResponse userFundAccountInfoDO : apiResponse.getData()) {
                    if (StringUtils.isNotBlank(userFundAccountInfoDO.getAccountNumber())) {
                        OrderDividendSyncRequest request = new OrderDividendSyncRequest();
                        request.setUserId(userFundAccountInfoDO.getUserId());
                        request.setAccountNumber(userFundAccountInfoDO.getAccountNumber());
                        request.setChannel(FundChannelEnum.YIFENG);
                        dividendBiz.syncDividendOrder(request);
                        total++;
                    }
                }
                pageNo++;
                apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo,pageSize,FundChannelEnum.YIFENG);
            }
            resultMsg = String.format("共同步%s用户",total);
        } catch (Exception e) {
            log.error("红利再投订单同步异常",e);
            result=Boolean.FALSE;
            resultMsg="同步任务异常";
        }
        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        log.info("基金红利再投同步任务处理耗时：{}", System.currentTimeMillis() - startTime);
        traceContextOffSpan();
        return true;
    }
}
