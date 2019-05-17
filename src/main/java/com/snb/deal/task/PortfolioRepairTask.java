package com.snb.deal.task;

import com.jianlc.schedule.model.ScheduleExecuteRecord;
import com.jianlc.schedule.model.ScheduleServer;
import com.snb.deal.api.dto.redeem.OrderRedeemAmountResponse;
import com.snb.deal.bo.order.OrderRedeemBO;
import com.snb.deal.enums.FlowNumberTypeEnum;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderRedeemService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.api.plan.FundPlanService;
import com.snb.third.yifeng.dto.order.redeem.RedeemPortfolioRequest;
import com.snb.third.yifeng.dto.order.redeem.RedeemPortfolioResponse;
import com.snb.third.yifeng.dto.plan.YfBasePlanRequest;
import com.snb.third.yifeng.dto.plan.YfBasePlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Service("portfolioRepairTask")
@Slf4j
public class PortfolioRepairTask extends ScheduleTaskDealAbstract{

    @Resource
    FundPlanService fundPlanService;

    @Resource
    private OrderRedeemService orderRedeemService;

    @Resource
    private FundPortfolioService fundPortfolioService;

    @Resource
    private FlowNumberService flowNumberService;

    @Override
    public boolean execute(ScheduleServer scheduleServer) {
        long startTime = System.currentTimeMillis();
        ScheduleExecuteRecord scheduleExecuteRecord = new ScheduleExecuteRecord();
        scheduleExecuteRecord.setExecuteTime(new Date());
        Integer successNum=0,failedNum=0;
        Boolean result = Boolean.TRUE;
        String resultMsg = "处理完成";
        try {

            //暂停用户计划
            YfBasePlanRequest yfBasePlanRequest = new YfBasePlanRequest();
            yfBasePlanRequest.setAccountNumber("WLJ20180729000052630");
            yfBasePlanRequest.setRspId(41719);

            log.info("开始暂停用户定投计划：{}", yfBasePlanRequest);

            BaseResponse<YfBasePlanResponse> yfbaseResponse = null;
            try {
                yfbaseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.suspendPortfolioPlan(yfBasePlanRequest);
            } catch (Exception e) {
                log.error("暂停定投计划异常", e);
            }

            log.info("暂停用户定投计划完成响应：{}", yfbaseResponse);

            //对用户持仓发起赎回
            OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
            orderRedeemBO.setPortfolioId("1021083");
            orderRedeemBO.setAccountNumber("WLJ20180729000052630");

            OrderRedeemAmountResponse orderRedeemAmountResponse = orderRedeemService.getOrderRedeemAmount(orderRedeemBO);

            log.info("查询持仓：{}信息：{}","1021083",orderRedeemAmountResponse);

            if (orderRedeemAmountResponse != null) {

                if (orderRedeemAmountResponse.getAvailableAmount().compareTo(BigDecimal.ZERO) > 0) {
                    RedeemPortfolioRequest redeemPortfolioRequest = new RedeemPortfolioRequest();
                    redeemPortfolioRequest.setAccountNumber("WLJ20180729000052630");
                    redeemPortfolioRequest.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
                    redeemPortfolioRequest.setPortfolioId(1021083L);
                    redeemPortfolioRequest.setRedemptionAmount(orderRedeemAmountResponse.getAvailableAmount());
                    redeemPortfolioRequest.setInvestorPayId(24161);
                    redeemPortfolioRequest.setNotifyUrl("https://h5.ifastpss.com.cn/jlc/api///callback/ifast/orderCallback");
                    log.info("发送赎回请求开始-redeemPortfolioRequest-{}", redeemPortfolioRequest.toString());
                    BaseResponse<RedeemPortfolioResponse> baseResponse = (BaseResponse<RedeemPortfolioResponse>) fundPortfolioService.redeemPortfolio(redeemPortfolioRequest);
                    log.info("发送赎回请求结束-baseResponse-{}", baseResponse.toString());
                }
            }


        } catch (Exception e) {
            log.error("同步定投计划执行记录",e);
            result = Boolean.FALSE;
            resultMsg="任务执行异常";
        }
        resultMsg = String.format(resultMsg,successNum,failedNum);
        saveScheduleResult(scheduleExecuteRecord,scheduleServer,startTime,result,resultMsg);
        traceContextOffSpan();
        return true;
    }
}
