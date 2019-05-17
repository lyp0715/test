package com.snb.deal.mapper.portfolio;

import com.snb.deal.entity.portfolio.PortfolioDailyIncomeDO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PortfolioDailyIncomeMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PortfolioDailyIncomeDO record);

    PortfolioDailyIncomeDO selectUserIncomeByShowedDate(@Param("userId") String userId, @Param("planPortfolioRelId") Long planPortfolioRelId, @Param("showedDate") Date showedDate);

    int inesrtBatch(List<PortfolioDailyIncomeDO> portfolioDailyIncomeDOList);

    int updateBatch(List<PortfolioDailyIncomeDO> portfolioDailyIncomeDOList);


    /**
     * 根据用户id和计划持仓id查询最近一条持仓每日收益信息
     *
     * @param userId
     * @param planPortfolioRelId
     * @return
     * @author yunpeng.zhang
     */
    PortfolioDailyIncomeDO selectByUpdateDateAndShowedDate(@Param("userId") String userId, @Param("planPortfolioRelId") Long planPortfolioRelId);
}