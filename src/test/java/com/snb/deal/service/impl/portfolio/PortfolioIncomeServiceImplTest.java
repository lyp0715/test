package com.snb.deal.service.impl.portfolio;

import com.snb.BaseBeanTest;
import com.snb.deal.service.portfolio.PortfolioIncomeService;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Date;

public class PortfolioIncomeServiceImplTest extends BaseBeanTest{

    @Resource
    PortfolioIncomeService portfolioIncomeService;

    @Test
    public void syncPortfolioIncome() throws Exception {

        portfolioIncomeService.syncPortfolioIncome("da4ef8035a014063ae7dac0888cbc9a7","JLC20180510000027805",new Date());
    }

}