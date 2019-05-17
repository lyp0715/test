package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AutoInvestCompleteMessage;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 自动定投完成，更新计划持仓ID
 */
@Slf4j
public class PlanPortfolioIdComsumer extends ReliabilityEventMessageHandlerAdaptor<AutoInvestCompleteMessage> {

    @Resource
    private PlanService planService;

    @Override
    protected void handleData(AutoInvestCompleteMessage message) throws Exception {
        log.info("更新计划持仓ID消息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        String userId = message.getUserId();
        Long planInfoId = message.getPlanInfoId();
        String portfolioId = message.getThirdPortfolioId();

        if (StringUtils.isBlank(userId)
                || Objects.isNull(planInfoId) || StringUtils.isBlank(portfolioId)) {
            return;
        }

        planService.updatePlanThirdPortfolioId(userId,planInfoId,portfolioId, FundChannelEnum.YIFENG);

    }
}
