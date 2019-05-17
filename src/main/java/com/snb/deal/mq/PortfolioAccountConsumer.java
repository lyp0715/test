package com.snb.deal.mq;

import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.mq.bean.PortfolioIncome;
import com.snb.common.mq.bean.PortfolioIncomeMessage;
import com.snb.deal.entity.plan.PlanPortfolioAccountDO;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 接收收益更新信息，更新持仓账户中的收益数据
 */
@Slf4j
public class PortfolioAccountConsumer extends ReliabilityEventMessageHandlerAdaptor<PortfolioIncomeMessage> {
    @Resource
    PlanService planService;

    @Override
    protected void retryHandleData(PortfolioIncomeMessage portfolioIncomeMessage) throws Exception {

    }

    @Override
    protected void handleData(PortfolioIncomeMessage portfolioIncomeMessage) throws Exception {
        log.info("接收到收益更新消息：{}",portfolioIncomeMessage);
        try {
            String userId = portfolioIncomeMessage.getUserId();
            Long planPortfolioRelId = portfolioIncomeMessage.getPlanPortfolioRelId();

            if (CollectionUtils.isNotEmpty(portfolioIncomeMessage.getPortfolioIncomeList())) {
                PlanPortfolioAccountDO planPortfolioAccount = planService.queryUserAccountByPlanPortfolioRelId(planPortfolioRelId);

                if (Objects.isNull(planPortfolioAccount)) {
                    log.error("计划：{}账户，未查询到用户持仓账户",planPortfolioRelId);
                    return;
                }
                List<PortfolioIncome> portfolioIncomeList = portfolioIncomeMessage.getPortfolioIncomeList();
                //获取最后一天收益
                PortfolioIncome portfolioIncome = portfolioIncomeList.get(portfolioIncomeList.size()-1);

                PlanPortfolioAccountDO account = new PlanPortfolioAccountDO();
                account.setPlanPortfolioAccountId(planPortfolioAccount.getPlanPortfolioAccountId());
                account.setTotalPerformance(portfolioIncome.getAccumulatedPerformance());
                account.setTotalIncome(portfolioIncome.getAccumulatedProfitLoss());

                planService.updatePlanPortfolioAccount(account);
            }


        } catch (Exception e) {
            log.error("更新用户持仓账户异常",e);
        }
    }
}
