package com.snb.deal.service.impl.portfolio;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.entity.plan.PlanPortfolioRelDO;
import com.snb.deal.entity.portfolio.PortfolioDailyIncomeDO;
import com.snb.deal.mapper.plan.PlanPortfolioRelMapper;
import com.snb.deal.mapper.portfolio.PortfolioDailyIncomeMapper;
import com.snb.deal.service.portfolio.PortfolioIncomeService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.plan.FundPlanService;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeData;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeRequest;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PortfolioIncomeServiceImpl implements PortfolioIncomeService {

    @Autowired
    private PlanPortfolioRelMapper planPortfolioRelMapper;
    @Autowired
    private PortfolioDailyIncomeMapper portfolioDailyIncomeMapper;

    @Resource
    FundPlanService fundPlanService;
    @Autowired
    private GuidCreater guidCreater;

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void syncPortfolioIncome(String userId, String accountNumber, Date time) throws Exception {

        //查询用户组合持仓信息
        List<PlanPortfolioRelDO> planPortfolioRelDOList = planPortfolioRelMapper.selectByUserIdAndChannel(userId, FundChannelEnum.YIFENG.getChannel());

        if (CollectionUtils.isEmpty(planPortfolioRelDOList)) {
            log.info("同步用户：{}组合收益，无计划持仓信息",userId);
            return;
        }

//        Preconditions.checkState(CollectionUtils.isNotEmpty(planPortfolioRelDOList),"未查询到用户:%s计划持仓列表",userId);

        //目前仅支持一个组合
        PlanPortfolioRelDO planPortfolioRelDO = planPortfolioRelDOList.get(0);

        String thirdPortfolioId = planPortfolioRelDO.getThirdPortfolioId();

        //没有组合ID
        if (StringUtils.isBlank(thirdPortfolioId)) {
            log.info("同步用户：{}组合收益，无持仓ID",userId);
            return;
        }

//        Preconditions.checkState(StringUtils.isNotBlank(planPortfolioRelDO.getThirdPortfolioId()),"用户:%s计划持仓:%s无持仓ID",userId,planPortfolioRelDO.getThirdPortfolioId());

        //往前查询14天
        String showEndDate = new DateTime(time).toString("yyyy-MM-dd");
        String showStartDate = new DateTime(time).minusDays(14).toString("yyyy-MM-dd");

        YfPortfolioIncomeRequest incomeRequest = YfPortfolioIncomeRequest.builder()
                .accountNumber(accountNumber)
                .portfolioId(Integer.valueOf(planPortfolioRelDO.getThirdPortfolioId()))
                .showedDateStart(showStartDate)
                .showedDateEnd(showEndDate)
                .build();

        log.info("查询用户：{}，组合：{}收益：{}",userId,planPortfolioRelDO.getThirdPortfolioId(),incomeRequest);

        BaseResponse<YfPortfolioIncomeResponse> baseResponse = (BaseResponse<YfPortfolioIncomeResponse>) fundPlanService.queryPortfolioIncome(incomeRequest);

        Preconditions.checkNotNull(baseResponse,"查询用户:%s,持仓:%s,%s收益无响应",userId,thirdPortfolioId,showStartDate);

        Preconditions.checkState(baseResponse.success(),"查询用户:%s,持仓:%s,%s收益响应失败:%s",userId,thirdPortfolioId,showStartDate,baseResponse.getMessage());

        if (!Objects.isNull(baseResponse.getData()) && CollectionUtils.isNotEmpty(baseResponse.getData().getData())) {

            List<PortfolioDailyIncomeDO> updateList = Lists.newArrayList();
            List<PortfolioDailyIncomeDO> insertList = Lists.newArrayList();

            //查询到数据
            for (YfPortfolioIncomeData yfPortfolioIncomeData : baseResponse.getData().getData()) {

                PortfolioDailyIncomeDO incomeDO = new PortfolioDailyIncomeDO();
                incomeDO.setAccumulatedPerformance(yfPortfolioIncomeData.getAccumulatedPerformance());
                incomeDO.setAccumulatedProfitloss(yfPortfolioIncomeData.getAccumulatedProfitLoss());
                incomeDO.setPerformanceDaily(yfPortfolioIncomeData.getPerformanceDaily());
                incomeDO.setProfitlossDaily(yfPortfolioIncomeData.getProfitLossDaily());
                incomeDO.setShowedDate(new DateTime(Long.valueOf(yfPortfolioIncomeData.getShowedDate())).toDate());
                if (StringUtils.isNotEmpty(yfPortfolioIncomeData.getUpdatedDate())) {
                    incomeDO.setUpdatedDate(new DateTime(Long.valueOf(yfPortfolioIncomeData.getUpdatedDate())).toDate());
                }

                //查询是否有当天的收益
                PortfolioDailyIncomeDO portfolioDailyIncomeDO = portfolioDailyIncomeMapper.selectUserIncomeByShowedDate(userId,planPortfolioRelDO.getPlanPortfolioRelId(),incomeDO.getShowedDate());

                if (Objects.isNull(portfolioDailyIncomeDO)) {
                    //插入
                    incomeDO.setUserId(userId);
                    incomeDO.setPlanPortfolioRelId(planPortfolioRelDO.getPlanPortfolioRelId());
                    incomeDO.setUserPortfolioDailyIncomeId(guidCreater.getUniqueID());
                    insertList.add(incomeDO);

                } else {
                    //更新
                    incomeDO.setUserPortfolioDailyIncomeId(portfolioDailyIncomeDO.getUserPortfolioDailyIncomeId());
                    updateList.add(incomeDO);
                }

            }

            if (CollectionUtils.isNotEmpty(insertList)) {
                portfolioDailyIncomeMapper.inesrtBatch(insertList);
            }

            if (CollectionUtils.isNotEmpty(updateList)) {
                portfolioDailyIncomeMapper.updateBatch(updateList);
            }
        }

    }
}
