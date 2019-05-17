package com.snb.deal.biz.invest;


import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.invest.AutoInvestRequest;
import com.snb.deal.api.dto.invest.InvestRequest;
import com.snb.deal.api.dto.invest.InvestResponse;
import com.snb.third.yifeng.dto.order.invest.YfBuyTransactionDateResponse;
import com.snb.third.yifeng.dto.order.invest.YfPurchasePortfolioTransactionFeeResponse;

import java.math.BigDecimal;

public interface InvestBiz {

    /**
     * 投资
     *
     * @param investRequest
     * @return
     */
    APIResponse<InvestResponse> invest(InvestRequest investRequest);

    /**
     * 根据计划id查询买入预计确认日期和买入预计交易日期
     * 奕丰接口：交易类API-获取购买预计交易日期和交易确认日期
     *
     * @param fundCodes 基金编码，例如 1,2,3
     * @return 买入预计确认日期和买入预计交易日期
     * @author yunpeng.zhang
     */
    YfBuyTransactionDateResponse getBuyTransactionAndExpectedConfirmedDate(String fundCodes) throws Exception;

    /**
     * 根据购买金额和组合代码估算相关的申/认购费/费率/折扣费率
     * 奕丰接口：投资组合类API-获取组合购买费率和费用
     *
     * @param portfolioCode    组合代码
     * @param investmentAmount 买入金额
     * @return
     * @author yunpeng.zhang
     */
    YfPurchasePortfolioTransactionFeeResponse getOrderInvestFee(String portfolioCode, BigDecimal investmentAmount) throws Exception;
    /**
     * 同步投资订单
     * @throws Exception
     */
    void syncInvestOrder() throws Exception;

    /**
     * 自动定投
     * @param request
     * @return
     */
    void autoInvest(AutoInvestRequest request) throws Exception;

    /**
     * 根据订单号同步订单
     * @param merchantNumber
     * @throws Exception
     */
    void syncInvestOrder(String merchantNumber, FundChannelEnum channel) throws Exception;

    /**
     * 用户是否投资过
     * @param userId
     * @return
     */
    Boolean isInvested(String userId);
}
