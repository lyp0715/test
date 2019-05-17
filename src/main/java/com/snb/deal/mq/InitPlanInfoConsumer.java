package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.FundAccountOpenSuccessMessage;
import com.snb.common.mq.bean.FundAccountRiskMessage;
import com.snb.deal.biz.plan.PlanBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;

/**
 * 开户完成后，初始化用户计划信息
 */
@Slf4j
public class InitPlanInfoConsumer extends ReliabilityEventMessageHandlerAdaptor<FundAccountRiskMessage> {

    @Resource
    private PlanBiz planBiz;

    @Override
    protected void handleData(FundAccountRiskMessage message) throws Exception {

        log.info("风险测评完成，收到初始化定投计划消息：{}",message);

        String userId = message.getUserId();

        if (StringUtils.isBlank(userId)) {
            return;
        }

        planBiz.initPlan(userId);

    }
}
