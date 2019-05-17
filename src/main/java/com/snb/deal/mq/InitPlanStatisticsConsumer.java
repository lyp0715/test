package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.mq.bean.PlanCreatedMessage;
import com.snb.deal.entity.plan.PlanStatisticsDO;
import com.snb.deal.mapper.plan.PlanStatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 计划创建完成，初始化计划统计信息
 */
@Slf4j
public class InitPlanStatisticsConsumer extends ReliabilityEventMessageHandlerAdaptor<PlanCreatedMessage> {

    @Autowired
    private PlanStatisticsMapper planStatisticsMapper;
    @Autowired
    private GuidCreater guidCreater;

    @Override
    protected void handleData(PlanCreatedMessage message) throws Exception {

        log.info("计划创建完成，初始化统计信息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        String userId = message.getUserId();
        Long planId = message.getPlanId();

        if (StringUtils.isEmpty(userId) || Objects.isNull(planId)) {
            return;
        }

        PlanStatisticsDO planStatisticsDO = planStatisticsMapper.selectByPlanInfoId(planId,userId);

        if (Objects.isNull(planStatisticsDO)) {
            //初始化计划统计信息
            PlanStatisticsDO planStatistics = new PlanStatisticsDO();
            planStatistics.setPlanId(message.getPlanId());
            planStatistics.setPlanStatisticsId(guidCreater.getUniqueID());
            planStatistics.setUserId(message.getUserId());

            planStatisticsMapper.insert(planStatistics);
        }

    }
}
