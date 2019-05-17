package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.deal.api.dto.invest.AutoInvestRequest;
import com.snb.deal.biz.invest.InvestBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 定投计划执行回调消费
 */
@Slf4j
public class AutoInvestConsumer extends ReliabilityEventMessageHandlerAdaptor<AutoInvestMessage> {

    @Resource
    private InvestBiz investBiz;

    @Override
    protected void handleData(AutoInvestMessage autoInvestMessage) throws Exception {

        log.info("收到定投执行回调消息：{}",autoInvestMessage);

        if (Objects.isNull(autoInvestMessage) || StringUtils.isEmpty(autoInvestMessage.getThirdPlanId())
                || StringUtils.isEmpty(autoInvestMessage.getMerchantNumber())) {
            return;
        }

        if (Objects.isNull(autoInvestMessage.getChannel())) {
            log.error("定投回调消息处理失败，未知渠道");
            return;
        }

        AutoInvestRequest request = new AutoInvestRequest();
        request.setThirdPlanId(autoInvestMessage.getThirdPlanId());
        request.setMerchantNumber(autoInvestMessage.getMerchantNumber());
        request.setChannel(autoInvestMessage.getChannel());
        if (StringUtils.isNotEmpty(autoInvestMessage.getNextRunDate())) {
            request.setNextRunDate(new DateTime(Long.valueOf(autoInvestMessage.getNextRunDate())).toDate());
        }

        investBiz.autoInvest(request);
    }
}
