package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.AccountSyncMessage;
import com.snb.deal.biz.plan.PlanBiz;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 组合账户同步
 */
@Slf4j
public class SyncPortfolioAccountConsumer extends ReliabilityEventMessageHandlerAdaptor<AccountSyncMessage> {

    @Resource
    private PlanBiz planBiz;

    @Override
    protected void handleData(AccountSyncMessage message) throws Exception {

        log.info("收到组合账户同步消息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        String userId = message.getUserId();
        String accountNumber = message.getAccountNumber();

        if (StringUtils.isBlank(userId) || StringUtils.isEmpty(accountNumber)) {
            log.error("同步组合账户，参数异常，userId={},accountNunber={}",userId,accountNumber);
            return;
        }
        planBiz.syncPortfolioAccount(message);

    }
}
