package com.snb.deal.mq;

import com.google.common.base.Preconditions;
import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AddBankCardSuccessMessage;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 用户换卡完成后，更新计划的支付代码
 */
@Slf4j
public class ModifyInvestorPayIdConsumer extends ReliabilityEventMessageHandlerAdaptor<AddBankCardSuccessMessage> {
    @Resource
    private PlanBiz planBiz;

    @Override
    protected void handleData(AddBankCardSuccessMessage addBankCardSuccessMessage) throws Exception {
        log.info("收到更新用户计划支付代码消息：{}",addBankCardSuccessMessage);
        if (Objects.isNull(addBankCardSuccessMessage)) {
            return;
        }

        String userId = addBankCardSuccessMessage.getUserId();
        Integer investorPayId = addBankCardSuccessMessage.getInvestPayId();

        if (StringUtils.isBlank(userId) || Objects.isNull(investorPayId) || investorPayId<=0) {
            return;
        }

        planBiz.modifyInvestorPayId(userId,investorPayId);

        log.info("更新用户:{}计划支付代码完成",userId);
    }
}
