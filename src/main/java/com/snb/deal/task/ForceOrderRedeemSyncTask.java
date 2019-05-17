package com.snb.deal.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 强制赎回订单同步任务
 */
@Service("forceOrderRedeemSyncTask")
@Slf4j
public class ForceOrderRedeemSyncTask extends ScheduleTaskDealAbstract {

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Resource
    private OrderRedeemBiz orderRedeemBiz;
    @Resource
    private Environment environment;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        long startTime = System.currentTimeMillis();
        log.info("基金强制赎回同步任务处理开始：startTime:{}", startTime);
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());

        int pageNo = environment.getProperty("redeem.syncForceRedeemOrder.pageNo", Integer.class, 0);
        int pageSize = environment.getProperty("redeem.syncForceRedeemOrder.pageSize", Integer.class, 1000);
        int total = 0;
        Boolean result = Boolean.TRUE;
        String resultMsg = "";
        try {
            //1. 查询已经开户用户列表
            APIResponse<List<GetUserFundAccountInfoResponse>> apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo, pageSize, FundChannelEnum.YIFENG);
            while (apiResponse.isSuccess() && CollectionUtils.isNotEmpty(apiResponse.getData())) {
                for (GetUserFundAccountInfoResponse userFundAccountInfoDO : apiResponse.getData()) {
                    orderRedeemBiz.syncForceOrderRedeem(userFundAccountInfoDO.getUserId(), userFundAccountInfoDO.getAccountNumber());
                    total++;
                }
                pageNo++;
                apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo, pageSize, FundChannelEnum.YIFENG);
            }
            resultMsg = String.format("共同步%s用户", total);
        } catch (Exception e) {
            result = Boolean.FALSE;
            log.error("基金强制赎回同步任务处理失败！",e);
            resultMsg = "基金强制赎回同步任务处理失败！";
        }
        saveScheduleResult(scheduleExecuteRecord, scheduleServer, startTime, result, resultMsg);
        log.info("基金强制赎回同步任务处理耗时：{}", System.currentTimeMillis() - startTime);
        traceContextOffSpan();
        return true;
    }
}
