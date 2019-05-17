package com.snb.deal.service.portfolio;

import java.util.Date;

public interface PortfolioIncomeService {

    /**
     * 同步用户组合收益
     * @param userId
     * @param accountNumber
     * @param time
     * @throws Exception
     */
    void syncPortfolioIncome(String userId, String accountNumber, Date time) throws Exception;

}
