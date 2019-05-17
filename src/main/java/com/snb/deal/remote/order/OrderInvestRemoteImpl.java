package com.snb.deal.remote.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.api.dto.invest.*;
import com.snb.deal.api.remote.order.OrderInvestRemote;
import com.snb.deal.biz.invest.InvestBiz;
import com.snb.deal.bo.order.OrderInvestCombinationRateBO;
import com.snb.deal.enums.ResultCode;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailDTO;
import com.snb.fund.api.remote.FundMainModelRemote;
import com.snb.third.yifeng.dto.order.invest.YfBuyTransactionDateResponse;
import com.snb.third.yifeng.dto.order.invest.YfPurchasePortfolioTransactionFeeDetail;
import com.snb.third.yifeng.dto.order.invest.YfPurchasePortfolioTransactionFeeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service(version = "1.0")
public class OrderInvestRemoteImpl implements OrderInvestRemote {
    @Resource
    private InvestBiz investBiz;

    @Reference(version = "1.0")
    private FundMainModelRemote fundMainModelRemote;

    /**
     * 单笔买入
     *
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<InvestResponse> singleInvest(InvestRequest request) {
        return investBiz.invest(request);
    }

    /**
     * 根据基金编码查询买入预计确认日期和买入预计交易日期，返回数据可能为空
     *
     * @param fundCodes 基金编码，例如 1,2,3
     * @return 买入预计确认日期和买入预计交易日期
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<OrderInvestExpectedDateResponse> getBuyTransactionAndExpectedConfirmedDate(String fundCodes) {
        //fixme 可考虑加入缓存
        log.info("查询买入预计确认日期和买入预计交易日期，fundCodes：{}", fundCodes);

        //0. 参数校验
        if (StringUtils.isEmpty(fundCodes)) {
            log.error("查询买入预计确认日期和买入预计交易日期参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 获取买入预计确认日期和买入预计交易日期
        YfBuyTransactionDateResponse yfBuyTransactionDateResponse;
        try {
            yfBuyTransactionDateResponse = investBiz.getBuyTransactionAndExpectedConfirmedDate(fundCodes);
        } catch (Exception e) {
            log.error("查询买入预计交易日期和预计交易确认日期异常！基金代码：{}", fundCodes, e);
            return APIResponse.build(ResultCode.BUY_TRANSACTION_AND_EXPECTED_CONFIRMED_DATE_ERROR);
        }
        // 组织数据
        OrderInvestExpectedDateResponse orderInvestExpectedDateResponse = new OrderInvestExpectedDateResponse();
        if (yfBuyTransactionDateResponse == null) {
            log.info("没有查询到买入预计确认日期和买入预计交易日期！基金代码：{}", fundCodes);
        } else {
            Date investExpectedDate = DateTimeUtil.parseDate(yfBuyTransactionDateResponse.getTransactionDate());
            Date expectedConfirmedDate = DateTimeUtil.parseDate(yfBuyTransactionDateResponse.getExpectedCofirmedDate());
            orderInvestExpectedDateResponse.setInvestExpectedDate(investExpectedDate);
            orderInvestExpectedDateResponse.setInvestExpectedConfirmedDate(expectedConfirmedDate);
            // 买入收益到账日期现在指的是收益产生日期，即交易预计确认日期，另外还有一个概念查看收益日期=交易确认日期+1个自然日
            if (expectedConfirmedDate != null) {
                LocalDate expectedIncomeDateLocalDate = DateTimeUtil.toLocalDate(expectedConfirmedDate).plusDays(1);
                orderInvestExpectedDateResponse.setInvestExpectedIncomeDate(DateTimeUtil.toDate(expectedIncomeDateLocalDate));
            }

        }
        return APIResponse.build(SystemResultCode.SUCCESS).setData(orderInvestExpectedDateResponse);
    }

    /**
     * 根据购买金额和组合代码估算相关的申/认购费/组合费率/组合折扣后费率
     *
     * @param orderInvestFeeRequest 包含购买金额和组合代码
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<OrderInvestFeeResponse> getOrderInvestFee(OrderInvestFeeRequest orderInvestFeeRequest) {
        log.info("查询买入相关费用，request：{}", JSON.toJSONString(orderInvestFeeRequest));
        OrderInvestFeeResponse orderInvestFeeResponse = new OrderInvestFeeResponse();

        //0. 参数校验
        if (orderInvestFeeRequest == null || orderInvestFeeRequest.getInvestmentAmount() == null
                || StringUtils.isEmpty(orderInvestFeeRequest.getPortfolioCode())
                || orderInvestFeeRequest.getInvestmentAmount().compareTo(BigDecimal.ZERO)==0) {
            log.error("查询买入相关费用参数校验失败！request：{}", JSON.toJSONString(orderInvestFeeRequest));
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 查询数据
        YfPurchasePortfolioTransactionFeeResponse orderInvestFee;
        try {
            orderInvestFee = investBiz.getOrderInvestFee(orderInvestFeeRequest.getPortfolioCode(), orderInvestFeeRequest.getInvestmentAmount());
        } catch (Exception e) {
            log.error("查询买入相关费用调用第三方服务异常！组合代码：{}，买入金额：{}", orderInvestFeeRequest.getPortfolioCode(), orderInvestFeeRequest.getInvestmentAmount(), e);
            return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
        }

        //2. 组织数据
        if (orderInvestFee == null) {
            log.info("没有查询到买入相关费用！request：{}", JSON.toJSONString(orderInvestFeeRequest));
        } else {
            /*
            FundMainModelDetailRequest request = new FundMainModelDetailRequest();
            request.setThirdPortfolioCode(orderInvestFeeRequest.getPortfolioCode());
            APIResponse<List<FundMainModelDetailDTO>> fundDetailListResponse;
            try {
                fundDetailListResponse = fundMainModelRemote.queryFundMainModelDetailByCondition(request);
            } catch (Exception e) {
                log.error("查询买入相关费用，查询基金明细系统异常！组合代码：{}", orderInvestFeeRequest.getPortfolioCode(), e);
                return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
            }
            if (!fundDetailListResponse.isSuccess()) {
                log.info("查询买入相关费用，查询基金明细失败！组合代码：{}", orderInvestFeeRequest.getPortfolioCode());
                return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
            }
            List<FundMainModelDetailDTO> fundDetailList = fundDetailListResponse.getData();
            if (CollectionUtils.isEmpty(fundDetailList)) {
                log.info("查询买入相关费用，查询到基金明细为空！组合代码：{}", orderInvestFeeRequest.getPortfolioCode());
                return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
            }
            List<YfPurchasePortfolioTransactionFeeDetail> feeDetails = orderInvestFee.getFeeDetails();
            if (CollectionUtils.isEmpty(feeDetails)) {
                log.info("查询买入相关费用，查询到第三方买入费用明细为空！组合代码：{}", orderInvestFeeRequest.getPortfolioCode());
                return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
            }

            // 处理基金费率和费用
            OrderInvestCombinationRateBO orderInvestCombinationRateBO = handleFundDetailAndFee(fundDetailList, feeDetails);
            */

            // 处理基金费率和费用
            OrderInvestCombinationRateBO orderInvestCombinationRateBO = handleFee(orderInvestFee, orderInvestFeeRequest.getInvestmentAmount());

            if (orderInvestCombinationRateBO.getCombinationDiscountedRate().equals(BigDecimal.ZERO)) {
                log.info("查询买入相关费用，没有进行打折！request:{}", JSON.toJSONString(orderInvestFeeRequest));
                return APIResponse.build(ResultCode.ORDER_INVEST_FEE_ERROR);
            }

            // 折扣前估算得到的组合总费用。
            BigDecimal totalSalesCharge = new BigDecimal(orderInvestFee.getTotalSalesCharge());
            orderInvestFeeResponse.setTotalSalesCharge(totalSalesCharge);

            // 折扣后估算得到的组合总费用。
            BigDecimal totalDiscountSalesCharge = new BigDecimal(orderInvestFee.getTotalDiscountSalesCharge());
            orderInvestFeeResponse.setTotalDiscountSalesCharge(totalSalesCharge.subtract(totalDiscountSalesCharge));
            // 组合费率
            orderInvestFeeResponse.setCombinationRate(orderInvestCombinationRateBO.getCombinationRate());
            // 折后费率
            orderInvestFeeResponse.setDiscountedRate(orderInvestCombinationRateBO.getCombinationDiscountedRate());
            // 节省费用
            orderInvestFeeResponse.setSavingAmount(totalSalesCharge.subtract(totalDiscountSalesCharge));

        }
        return APIResponse.build(SystemResultCode.SUCCESS).setData(orderInvestFeeResponse);
    }

    /**
     * 根据主理人基金明细和第三方返回的买入基金费用计算买入组合费率和组织折后费率
     *
     * <pre>
     *     2018年5月12日：给出的计算公式计算结果和奕丰计算结果有偏差，参看：OrderInvestRemoteImpl#handleFee
     * </pre>
     * @param fundDetailList 主理人基金明细
     * @param feeDetails     第三方费用明细
     * @return
     * @author yunpeng.zhang
     */
    private OrderInvestCombinationRateBO handleFundDetailAndFee(List<FundMainModelDetailDTO> fundDetailList, List<YfPurchasePortfolioTransactionFeeDetail> feeDetails) {
        OrderInvestCombinationRateBO orderInvestCombinationRateBO = new OrderInvestCombinationRateBO();
        // 【组合费率】=单支A比例*单支A费率+单支B比例*单支B费率....
        BigDecimal combinationRate = BigDecimal.ZERO;
        // 【折后组合费率】=单支A比例*折后单支A费率+单支B比例*折后单支B费率...
        BigDecimal combinationDiscountedRate = new BigDecimal(0);

        for (FundMainModelDetailDTO fundMainModelDetailDTO : fundDetailList) {
            for (YfPurchasePortfolioTransactionFeeDetail feeDetail : feeDetails) {
                if (Objects.equals(fundMainModelDetailDTO.getFundCode(), feeDetail.getFundCode())) {
                    // 单只基金组合费率
                    BigDecimal salesChargesRate = new BigDecimal(feeDetail.getSalesChargesRate()).divide(new BigDecimal(100));
                    // 单只基金打折费率
                    BigDecimal discountRate = new BigDecimal(feeDetail.getDiscountRate());
                    // 单只基折后费率 = 组合费率 * (1 - 单只基金打折费率)
                    BigDecimal discountedRate = salesChargesRate.multiply(BigDecimal.ONE.subtract(discountRate));
                    // 基金占比
                    BigDecimal holdAmountScale = fundMainModelDetailDTO.getHoldAmountScale().divide(new BigDecimal(100));

                    combinationRate = combinationRate.add(holdAmountScale.multiply(salesChargesRate));
                    combinationDiscountedRate = combinationDiscountedRate.add(holdAmountScale.multiply(discountedRate));
                }
            }
        }

        orderInvestCombinationRateBO.setCombinationDiscountedRate(combinationDiscountedRate.setScale(2, RoundingMode.HALF_DOWN));
        orderInvestCombinationRateBO.setCombinationRate(combinationRate.setScale(2, RoundingMode.HALF_DOWN));

        return orderInvestCombinationRateBO;
    }

    private OrderInvestCombinationRateBO handleFee(YfPurchasePortfolioTransactionFeeResponse response, BigDecimal investmentAmount) {
        log.info("买入预估费用：response[{}]，购买金额：[{}]", response, investmentAmount);
        OrderInvestCombinationRateBO orderInvestCombinationRateBO = new OrderInvestCombinationRateBO();
        // 总折扣费用
        String totalDiscountSalesChargeString = response.getTotalDiscountSalesCharge();
        // 总费用
        String totalSalesChargeString = response.getTotalSalesCharge();

        if (StringUtils.isEmpty(totalSalesChargeString) || Objects.equals("0", totalSalesChargeString) ||
                Objects.equals("0.00", totalSalesChargeString)) {
            orderInvestCombinationRateBO.setCombinationDiscountedRate(new BigDecimal("0.00"));
        } else {
            BigDecimal totalSalesCharge = new BigDecimal(totalSalesChargeString);
            BigDecimal combinationRate = totalSalesCharge.divide(investmentAmount,4, RoundingMode.HALF_UP).
                    multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
            orderInvestCombinationRateBO.setCombinationRate(combinationRate);
        }

        if (StringUtils.isEmpty(totalDiscountSalesChargeString) || Objects.equals("0", totalDiscountSalesChargeString) ||
                Objects.equals("0.00", totalDiscountSalesChargeString)) {
            orderInvestCombinationRateBO.setCombinationDiscountedRate(new BigDecimal("0.00"));
        } else {
            BigDecimal totalDiscountSalesCharge = new BigDecimal(totalDiscountSalesChargeString);
            // 组合折后购买费率
            BigDecimal totalDiscountedSalesRate = totalDiscountSalesCharge.divide(investmentAmount, 4, RoundingMode.HALF_UP).
                    multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
            orderInvestCombinationRateBO.setCombinationDiscountedRate(totalDiscountedSalesRate);
        }
        log.info("买入预估费用：[{}]", orderInvestCombinationRateBO);
        return orderInvestCombinationRateBO;
    }

    @Override
    public APIResponse<Boolean> isInvested(String userId) {
        return APIResponse.build(SystemResultCode.SUCCESS).setData(investBiz.isInvested(userId));
    }

}