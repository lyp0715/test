package com.snb.deal.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.service.portfolio.PortfolioIncomeService;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 组合每日收益同步任务
 */
@Slf4j
@Service("portfolioDailyIncomeSyncTask")
public class PortfolioDailyIncomeSyncTask extends ScheduleTaskDealAbstract {

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Resource
    private PortfolioIncomeService portfolioIncomeService;


    @Override
    public boolean execute(ScheduleServer scheduleServer) {

        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());
        long startTime = System.currentTimeMillis();

        Integer pageNo=0,pageSize=1000,success=0,failed=0;
        Boolean result = Boolean.TRUE;
        String resultMsg = "";

        try {
            //1. 查询已经开户用户列表
            Date now = new Date();
            log.info("开始同步：{}组合收益",new DateTime(now).toString("yyyy-MM-dd"));
            APIResponse<List<GetUserFundAccountInfoResponse>> apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo,pageSize,FundChannelEnum.YIFENG);
            while(apiResponse.isSuccess() && CollectionUtils.isNotEmpty(apiResponse.getData())) {

                for (GetUserFundAccountInfoResponse userFundAccountInfoDO : apiResponse.getData()) {

                    if (StringUtils.isNotBlank(userFundAccountInfoDO.getAccountNumber())) {

                        //查询
                        String userId = userFundAccountInfoDO.getUserId();
                        String accountNumber = userFundAccountInfoDO.getAccountNumber();

                        try {
                            portfolioIncomeService.syncPortfolioIncome(userId,accountNumber,now);
                            success++;
                        } catch (Exception e) {
                            log.error("同步用户：{}组合每日收益异常",userId,e);
                            failed++;
                        }
                    }
                }
                pageNo++;

                apiResponse = fundUserRemote.pageAvailableFundAccountInfo(pageNo,pageSize,FundChannelEnum.YIFENG);
            }

            resultMsg = String.format("成功同步%s，失败%s",success,failed);

        } catch (Exception e) {
            log.error("组合收益同步任务执行异常",e);
            resultMsg="组合收益同步任务执行异常";
        }

        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
