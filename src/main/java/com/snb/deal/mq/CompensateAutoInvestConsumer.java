package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.common.mq.bean.PlanExecuteRecordSyncMessage;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.plan.PlanExecuteRecordDO;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * 定投计划执行记录同步完成后，补偿定投订单（可能定投订单没有回调，此时进行手动补发）
 */
@Slf4j
public class CompensateAutoInvestConsumer extends ReliabilityEventMessageHandlerAdaptor<PlanExecuteRecordSyncMessage> {

    @Resource
    private PlanService planService;
    @Resource
    private OrderInvestService orderInvestService;

    @Override
    protected void handleData(PlanExecuteRecordSyncMessage message) throws Exception {

        log.info("定投回调补偿消息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        String userId = message.getUserId();
        Long planInfoId = message.getPlanInfoId();
        String thirdPlanId = message.getThirdPlanId();

        if (StringUtils.isBlank(userId) || Objects.isNull(planInfoId) || StringUtils.isBlank(thirdPlanId)) {
            return;
        }

        //查询用户最后一笔定投执行记录
        PlanExecuteRecordDO planExecuteRecordDO = planService.queryUserLastestExecuteRecord(userId,planInfoId);
        if (Objects.isNull(planExecuteRecordDO)) {
            log.info("定投回调补偿，没有查询到用户：{}计划：{}定投计划执行记录",userId,planInfoId);
            return;
        }

        //校验，如果执行记录是今天的，并且此订单在order_invest表中不存在，则手动补发定投回调
        String merchantNumber = planExecuteRecordDO.getMerchantNumber();
//        Date executeTime = planExecuteRecordDO.getExecuteTime();
//        String nowDate = DateTime.now().toString("yyyy-MM-dd");
//        String executeDate = new DateTime(executeTime).toString("yyyy-MM-dd");

//        if (!Objects.isNull(executeTime) && nowDate.equals(executeDate)) {

        OrderInvestDO investDO = orderInvestService.queryByMerchantNumber(merchantNumber, FundChannelEnum.YIFENG);
        if (Objects.isNull(investDO)) {
            log.info("定投回调补偿，订单号：{}",merchantNumber);
            AutoInvestMessage autoInvestMessage = new AutoInvestMessage();
            autoInvestMessage.setMerchantNumber(merchantNumber);
            autoInvestMessage.setThirdPlanId(thirdPlanId);
            autoInvestMessage.setChannel(FundChannelEnum.YIFENG);
            orderInvestService.compensateAutoInvestCallback(autoInvestMessage);
        } else {
            log.info("定投回调补偿，订单已经存在，merchantNumber：{}",merchantNumber);
        }
//        } else {
//            log.info("定投回调补偿，当天无有效执行记录，merchantNumber:{},executeDate:{}",merchantNumber,executeDate);
//        }
    }
}
