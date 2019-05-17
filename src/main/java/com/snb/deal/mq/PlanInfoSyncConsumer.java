package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.deal.biz.plan.PlanBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * 更新计划信息
 */
@Slf4j
public class PlanInfoSyncConsumer extends ReliabilityEventMessageHandlerAdaptor<AutoInvestMessage> {

    @Resource
    PlanBiz planBiz;

    @Override
    protected void handleData(AutoInvestMessage message) throws Exception {

        log.info("收到同步计划信息消息：{}",message);

        if (Objects.isNull(message) || StringUtils.isBlank(message.getThirdPlanId())) {
            return;
        }

        Date nextRunDate = null;

        if (StringUtils.isNotBlank(message.getNextRunDate())) {
            nextRunDate = new DateTime(Long.valueOf(message.getNextRunDate())).toDate();
        }

        planBiz.updatePlanNextRunDate(message.getThirdPlanId(),nextRunDate,message.getChannel());

    }
}
