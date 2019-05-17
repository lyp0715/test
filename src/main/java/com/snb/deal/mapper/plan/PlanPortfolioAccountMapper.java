package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanPortfolioAccountDO;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanPortfolioAccountMapper {

    int insert(PlanPortfolioAccountDO record);

    PlanPortfolioAccountDO selectByPrimaryKey(Long id);


    int update(PlanPortfolioAccountDO planPortfolioAccount);

    /**
     * 根据计划持仓关系id查询计划持仓账户信息
     *
     * @param planPortfolioRelId 计划持仓关系id
     * @return
     * @author yunpeng.zhang
     */
    PlanPortfolioAccountDO selectByPlanPortfolioRelId(Long planPortfolioRelId);
}