package com.snb.deal.biz.invest.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.invest.AutoInvestRequest;
import com.snb.deal.api.dto.invest.InvestRequest;
import com.snb.deal.api.dto.invest.InvestResponse;
import com.snb.deal.api.enums.order.InvestTypeEnum;
import com.snb.deal.biz.invest.InvestBiz;
import com.snb.deal.bo.order.OrderInvestBO;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.order.OrderRebalanceAuthDO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.entity.plan.PlanPortfolioModel;
import com.snb.deal.enums.*;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.order.OrderRebalanceService;
import com.snb.deal.service.plan.PlanService;
import com.snb.fund.api.dto.mainmodel.FundMainModelDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailRequest;
import com.snb.fund.api.dto.mainmodel.FundMainModelRequest;
import com.snb.fund.api.enums.FundMainModelStatusEnums;
import com.snb.fund.api.remote.FundMainModelRemote;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.api.order.invest.ThirdOrderInvestService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import com.snb.third.yifeng.dto.order.invest.*;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * 投资服务
 */
@Service
@Slf4j
public class InvestBizImpl implements InvestBiz {

    @Resource
    PlanService planService;
    @Resource
    OrderInvestService orderInvestService;
    @Resource
    FundPortfolioService fundPortfolioService;
    @Resource
    private ThirdOrderInvestService thirdOrderInvestService;
    @Resource
    private OrderRebalanceService orderRebalanceService;
    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Reference(version = "1.0")
    private FundMainModelRemote fundMainModelRemote;

    @Resource
    private Environment environment;

    @Override
    public APIResponse<InvestResponse> invest(InvestRequest investRequest) {
        InvestResponse investResponse = new InvestResponse();
        log.info("组合投资请求：{}", JSON.toJSONString(investRequest));
        String userId = investRequest.getUserId();
        Long planId = investRequest.getPlanId();
        //1. 查询用户计划持仓关系-确认有持仓id
        List<PlanPortfolioModel> planPortfolioModelList = planService.queryUserPlanPortfolioModel(userId);
        if (CollectionUtils.isEmpty(planPortfolioModelList)) {
            log.error("组合投资，查询用户：{}计划：{}持仓信息失败", userId, planId);
            return APIResponse.build(ResultCode.INVEST_NO_PORTFOLIO);
        }
        PlanPortfolioModel planPortfolioModel = planPortfolioModelList.get(0);

        if (StringUtils.isEmpty(planPortfolioModel.getThirdPortfolioId())) {
            log.error("组合投资，用户：{}计划：{}无持仓ID", userId, planId);
            return APIResponse.build(ResultCode.INVEST_NO_PORTFOLIOID);
        }

        //4. 初始化投资订单
        OrderInvestDO orderInvestDO = new OrderInvestDO();
        //用户id
        orderInvestDO.setUserId(userId);
        //投资金额
        orderInvestDO.setTransactionAmount(investRequest.getInvestAmount());
        //渠道
        orderInvestDO.setChannel(FundChannelEnum.YIFENG.getChannel());
        //第三方持仓id
        orderInvestDO.setThirdPortfolioId(String.valueOf(planPortfolioModel.getThirdPortfolioId()));
        //组合代码
        orderInvestDO.setPortfolioCode(planPortfolioModel.getThirdPortfolioCode());
        //投资类型-手动买入
        orderInvestDO.setInvestType(InvestTypeEnum.MANUAL.getType());
        //第三方账户
        orderInvestDO.setAccountNumber(investRequest.getFundUserAccount());


        try {
            orderInvestDO = orderInvestService.createInvestOrder(orderInvestDO);
        } catch (Exception e) {
            log.error("组合投资，保存用户：{}投资订单异常", userId, e);
            return APIResponse.build(ResultCode.INVEST_SAVE_ORDER_FAILED);
        }

        //5. 投资
        String nofityUrl = environment.getProperty("snb.api.url.domain") + "/" + environment.getProperty("ifast.order.callback.url");

        // 是否正处于授权后的调仓过程中
        Boolean result = isRebalanceProcess(userId);
        // 买入基金开关
        String switchBuyFund = environment.getProperty("invest.switchBuyFund");
        if (result && Objects.equals(switchBuyFund, "on")) {
            List<YfFundInvest> yfFundInvestList = new ArrayList<>();
            // 获取当前有效主理人模型下基金列表
            List<FundMainModelDetailDTO> fundMainModelDetailList;
            try {
                fundMainModelDetailList = getFundMainModelDetail();
            } catch (Exception e) {
                log.error("购买基金异常，用户：{}", userId, e);
                return APIResponse.build(ResultCode.INVEST_NO_FUND_MAIN_MODEL_DETAIL);
            }
            // 组织请求参数
            for (FundMainModelDetailDTO fundMainModelDetailDTO : fundMainModelDetailList) {
                YfFundInvest yfFundInvest = new YfFundInvest();
                yfFundInvest.setFundCode(fundMainModelDetailDTO.getFundCode());
                yfFundInvest.setInvestmentAmount(investRequest.getInvestAmount().multiply(fundMainModelDetailDTO.getHoldAmountScale()).
                        divide(new BigDecimal(100), BigDecimal.ROUND_HALF_DOWN));
                yfFundInvest.setPortfolioId(Integer.valueOf(orderInvestDO.getThirdPortfolioId()));
                yfFundInvestList.add(yfFundInvest);
            }

            YfFundInvestRequest yfFundInvestRequest = YfFundInvestRequest.builder()
                    .accountNumber(orderInvestDO.getAccountNumber())
                    .merchantNumber(orderInvestDO.getMerchantNumber())
                    .investorPayId(Integer.valueOf(investRequest.getInvestorPayId()))
                    .riskConfirmed(1)
                    .notifyUrl(nofityUrl)
                    .purchaseFunds(yfFundInvestList)
                    .build();
            BaseResponse<YfFundInvestResponse> baseResponse = null;
            try {
                baseResponse = (BaseResponse<YfFundInvestResponse>) fundPortfolioService.investFunds(yfFundInvestRequest);
            } catch (Exception e) {
                log.error("用户{}投资基金异常", userId, e);
            }

            log.info("用户：{}请求投资组合完成响应：{}", userId, baseResponse);
            investResponse.setOrderNo(orderInvestDO.getOrderNo());
            investResponse.setMerchantNumber(orderInvestDO.getMerchantNumber());

            //有响应，并且明确告知失败
            if (!Objects.isNull(baseResponse) && !baseResponse.success()) {
                //失败
                try {
                    orderInvestService.afterInvestFailed(orderInvestDO.getOrderInvestId(), orderInvestDO.getOrderNo(),
                            baseResponse.getCode(), baseResponse.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return APIResponse.build(ResultCode.INVEST_REQ_FAILED, investResponse);
            }
            //发起成功
            try {
                orderInvestService.afterFundInvestApply(orderInvestDO, baseResponse);
            } catch (Exception e) {
                log.error("用户：{}请求投资组合完成后异常", userId, e);
            }
            // 获取买入结果
            Boolean isSuccess = getFundInvestResult(baseResponse);
            if (!isSuccess) {
                return APIResponse.build(ResultCode.INVEST_FAIL, investResponse);
            }

            // 计算订单预计确认日期（订单明细中最大的预计确认日期（和收益到账日期
            Date expectedConfirmDate = getOrderInvestExpectedConfirmDate(baseResponse);
            if (expectedConfirmDate == null) {
                log.error("用户：{}，获取订单预计确认日期失败！", userId);
            }

            //避免份额确认日期和收益确认日期展示为周末 FIXME 节假日待处理
            expectedConfirmDate = caculateExpectedConfirmDate(expectedConfirmDate);

            investResponse.setExpectedConfirmDate(expectedConfirmDate);
            if (expectedConfirmDate != null) {
                LocalDate expectedIncomeDateLocalDate = DateTimeUtil.toLocalDate(expectedConfirmDate).plusDays(1);
                Date expectedIncomeDate = DateTimeUtil.toDate(expectedIncomeDateLocalDate);
                expectedIncomeDate = caculateExpectedConfirmDate(expectedIncomeDate);
                investResponse.setInvestExpectedIncomeDate(expectedIncomeDate);
            }
        } else {
            YfInvestRequest yfInvestRequest = YfInvestRequest.builder().accountNumber(orderInvestDO.getAccountNumber())
                    .merchantNumber(orderInvestDO.getMerchantNumber()).portfolioCode(orderInvestDO.getPortfolioCode())
                    .portfolioId(Integer.valueOf(orderInvestDO.getThirdPortfolioId()))
                    .investmentAmount(investRequest.getInvestAmount())
                    .investorPayId(Integer.valueOf(investRequest.getInvestorPayId()))
                    .riskConfrimed(1)
                    .notifyUrl(nofityUrl)
                    .build();

            log.info("用户：{}开始投资组合：{}", userId, yfInvestRequest);

            BaseResponse<YfInvestResponse> baseResponse = null;

            try {
                baseResponse = (BaseResponse<YfInvestResponse>) fundPortfolioService.investPortfolio(yfInvestRequest);
            } catch (Exception e) {
                log.error("用户{}投资组合异常", userId, e);
            }

            log.info("用户：{}请求投资组合完成响应：{}", userId, baseResponse);
            investResponse.setOrderNo(orderInvestDO.getOrderNo());
            investResponse.setMerchantNumber(orderInvestDO.getMerchantNumber());

            //有响应，并且明确告知失败
            if (!Objects.isNull(baseResponse) && !baseResponse.success()) {
                //失败
                try {
                    orderInvestService.afterInvestFailed(orderInvestDO.getOrderInvestId(), orderInvestDO.getOrderNo(),
                            baseResponse.getCode(), baseResponse.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return APIResponse.build(ResultCode.INVEST_REQ_FAILED, investResponse);
            }
            //发起成功
            try {
                orderInvestService.afterInvestApply(orderInvestDO, baseResponse);
            } catch (Exception e) {
                log.error("用户：{}请求投资组合完成后异常", userId, e);
            }
            // 获取买入结果
            Boolean isSuccess = getInvestResult(baseResponse);
            if (!isSuccess) {
                return APIResponse.build(ResultCode.INVEST_FAIL, investResponse);
            }

            // 计算订单预计确认日期（订单明细中最大的预计确认日期（交易日期+基金申购确认日差））和收益到账日期
            if (orderInvestDO.getOrderInvestId() != null) {
                Date expectedConfirmDate = null;
                try {
                    expectedConfirmDate = orderInvestService.getOrderExpectedConfirmDate(orderInvestDO.getOrderInvestId());

                    //避免份额确认日期和收益确认日期展示为周末 FIXME 节假日待处理
                    expectedConfirmDate = caculateExpectedConfirmDate(expectedConfirmDate);

                } catch (Exception e) {
                    log.error("用户：{}，计算订单预计确认日期异常！", userId, e);
                }
                investResponse.setExpectedConfirmDate(expectedConfirmDate);
                if (expectedConfirmDate != null) {
                    LocalDate expectedIncomeDateLocalDate = DateTimeUtil.toLocalDate(expectedConfirmDate).plusDays(1);
                    Date expectedIncomeDate = DateTimeUtil.toDate(expectedIncomeDateLocalDate);
                    expectedIncomeDate = caculateExpectedConfirmDate(expectedIncomeDate);
                    investResponse.setInvestExpectedIncomeDate(expectedIncomeDate);
                }
            }
        }

        return APIResponse.build(SystemResultCode.SUCCESS, investResponse);
    }


    /**
     * 处理份额确认日期，避免日期在周末出现
     *
     * @param date
     * @return
     */
    public final static Date caculateExpectedConfirmDate(Date date) {

        if (null != date) {
            DateTime dateTime = new DateTime(date.getTime());
//            if (DateTimeConstants.FRIDAY == dateTime.getDayOfWeek()) {
//                return dateTime.plusDays(3).toDate();
//            }
            if (DateTimeConstants.SATURDAY == dateTime.getDayOfWeek()) {
                return dateTime.plusDays(2).toDate();
            }
            if (DateTimeConstants.SUNDAY == dateTime.getDayOfWeek()) {
                return dateTime.plusDays(1).toDate();
            }

        }
        return date;
    }

    private Date getOrderInvestExpectedConfirmDate(BaseResponse<YfFundInvestResponse> baseResponse) {
        if (Objects.isNull(baseResponse) || Objects.isNull(baseResponse.getData())) {
            return null;
        }

        List<YfFundInvestResponseDetail> yfFundInvestResponseDetailList = (List<YfFundInvestResponseDetail>) baseResponse.getData();
        if (CollectionUtils.isEmpty(yfFundInvestResponseDetailList)) {
            return null;
        }

        LocalDate resultDate = null;
        LocalDate expectedConfirmedLocalDate;
        for (YfFundInvestResponseDetail yfFundInvestResponseDetail : yfFundInvestResponseDetailList) {
            String expectedConfirmedDateString = yfFundInvestResponseDetail.getExpectedConfirmedDate();
            Date expectedConfirmedDate = DateTimeUtil.parseDate(expectedConfirmedDateString);
            if (expectedConfirmedDate != null) {
                expectedConfirmedLocalDate = DateTimeUtil.toLocalDate(expectedConfirmedDate);
                if (resultDate == null) {
                    resultDate = expectedConfirmedLocalDate;
                } else {
                    resultDate = resultDate.compareTo(expectedConfirmedLocalDate) > 0 ? resultDate : expectedConfirmedLocalDate;
                }
            }
        }
        if (resultDate == null) {
            return null;
        }

        return DateTimeUtil.toDate(resultDate);
    }

    private Boolean getFundInvestResult(BaseResponse<YfFundInvestResponse> baseResponse) {
        if (Objects.isNull(baseResponse) || Objects.isNull(baseResponse.getData())) {
            return false;
        }

        List<YfFundInvestResponseDetail> yfFundInvestResponseDetailList = (List<YfFundInvestResponseDetail>) baseResponse.getData();
        if (CollectionUtils.isEmpty(yfFundInvestResponseDetailList)) {
            return false;
        }
        // 失败次数
        int failCount = 0;
        for (YfFundInvestResponseDetail yfFundInvestResponseDetail : yfFundInvestResponseDetailList) {
            String transactionStatus = yfFundInvestResponseDetail.getTransactionStatus();
            if (Objects.equals(transactionStatus, String.valueOf(TransactionStatusEnum.FAIL.getCode())) ||
                    Objects.equals(transactionStatus, String.valueOf(TransactionStatusEnum.PAY_FAIL.getCode()))) {
                failCount++;
            }
        }
        // 若全部失败，则返回失败
        return failCount != yfFundInvestResponseDetailList.size();
    }

    /**
     * 获取主理人模型下的基金详情
     *
     * @author yunpeng.zhang
     */
    private List<FundMainModelDetailDTO> getFundMainModelDetail() {
        FundMainModelRequest fundMainModelRequest = new FundMainModelRequest();
        fundMainModelRequest.setFundMainModelStatusEnums(FundMainModelStatusEnums.EFFECTIVE);
        APIResponse<FundMainModelDTO> fundMainModelResponse = fundMainModelRemote.queryEffectiveFundMainModel(fundMainModelRequest);
        if (!fundMainModelResponse.isSuccess()) {
            log.info("获取有效主理人失败，原因：[{}]", fundMainModelResponse.getMsg());
            throw new RuntimeException("获取有效主理人模型失败");
        }
        FundMainModelDTO data = fundMainModelResponse.getData();
        Long fundMainModelId = data.getFundMainModelId();
        String thirdPortfolioCode = data.getThirdPortfolioCode();
        if (fundMainModelId == null || StringUtils.isEmpty(thirdPortfolioCode)) {
            log.info("获取有效主理人失败，fundMainModelResponse：[{}]", JSON.toJSONString(fundMainModelResponse));
            throw new RuntimeException("获取有效主理人模型失败");
        }

        FundMainModelDetailRequest fundMainModelDetailRequest = new FundMainModelDetailRequest();
        fundMainModelDetailRequest.setFundMainModelId(fundMainModelId);
        fundMainModelDetailRequest.setThirdPortfolioCode(thirdPortfolioCode);
        APIResponse<List<FundMainModelDetailDTO>> fundMainModelDetailListResponse = fundMainModelRemote.queryFundMainModelDetailByCondition(fundMainModelDetailRequest);
        if (!fundMainModelDetailListResponse.isSuccess()) {
            log.info("获取有效主理人详情失败，原因：[{}]", fundMainModelDetailListResponse.getMsg());
            throw new RuntimeException("获取有效主理人详情失败");
        }
        List<FundMainModelDetailDTO> fundMainModelDetailList = fundMainModelDetailListResponse.getData();
        if (CollectionUtils.isEmpty(fundMainModelDetailList)) {
            log.info("获取有效主理人详情为空，响应数据：[{}]", fundMainModelDetailListResponse);
            throw new RuntimeException("获取有效主理人详情失败");
        }
        return fundMainModelDetailList;
    }

    private Boolean isRebalanceProcess(String userId) {
        OrderRebalanceAuthDO orderRebalanceAuthDO = orderRebalanceService.getLastUnProcessRebalanceAuthByUserId(userId);
        if (orderRebalanceAuthDO == null) {
            return false;
        }
        Long orderRebalanceId = orderRebalanceAuthDO.getOrderRebalanceId();
        if (orderRebalanceId == null) {
            return false;
        }
        OrderRebalanceDO orderRebalanceDO = orderRebalanceService.getById(orderRebalanceId);
        if (orderRebalanceDO == null) {
            return false;
        }
        Integer transactionStatus = orderRebalanceDO.getTransactionStatus();
        return transactionStatus == OrderStatusEnum.PROCESS.getCode();

    }

    /**
     * 获取买入结果
     * <pre>
     *     解析三方买入响应，通过统计详情中交易失败或支付失败的次数，来判断买入是否失败；
     *     对于部分成功的买入，这里认为买入成功
     * </pre>
     *
     * @return
     * @author yunpeng.zhang
     */
    private Boolean getInvestResult(BaseResponse<YfInvestResponse> baseResponse) {
        if (Objects.isNull(baseResponse) || Objects.isNull(baseResponse.getData())) {
            return false;
        }

        YfInvestResponse data = baseResponse.getData();
        List<YfInvestResponseDetail> detailList = data.getDetails();
        if (CollectionUtils.isEmpty(detailList)) {
            return false;
        }
        // 失败次数
        int failCount = 0;
        for (YfInvestResponseDetail yfInvestResponseDetail : detailList) {
            String transactionStatus = yfInvestResponseDetail.getTransactionStatus();
            if (Objects.equals(transactionStatus, String.valueOf(TransactionStatusEnum.FAIL.getCode())) ||
                    Objects.equals(transactionStatus, String.valueOf(TransactionStatusEnum.PAY_FAIL.getCode()))) {
                failCount++;
            }
        }
        // 若全部失败，则返回失败
        return failCount != detailList.size();
    }

    @Override
    public void syncInvestOrder() throws Exception {

        //查询处理中投资订单
        String businessCode = OrderBusinessEnum.AUTO_INVEST.getCode() + "," + OrderBusinessEnum.MANUL_INVEST.getCode();
        List<OrderInvestBO> orderInvestBOList = orderInvestService.querySyncOrderList(businessCode, OrderStatusEnum.PROCESS.getCode(), FundChannelEnum.YIFENG.getChannel());

        if (CollectionUtils.isNotEmpty(orderInvestBOList)) {
            //final CountDownLatch latch = new CountDownLatch(orderInvestBOList.size());

            for (OrderInvestBO orderInvestBO : orderInvestBOList) {
                log.info("开始同步用户：{},订单：{}", orderInvestBO.getUserId(), orderInvestBO.getMerchantNumber());
                SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderInvestBO.getAccountNumber(),
                        orderInvestBO.getMerchantNumber());
                try {
                    BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                            fundPortfolioService.syncOrder(syncOrderRequest);
                    if (Objects.isNull(baseResponse)) {
                        log.error("同步用户：{},订单：{}，接口无响应", orderInvestBO.getUserId(), orderInvestBO.getMerchantNumber());
                        continue;
                    }

                    if (!baseResponse.success()) {
                        //订单查询失败
                        log.error("同步用户：{},订单：{}，查询失败：{}", orderInvestBO.getUserId(), orderInvestBO.getMerchantNumber(), baseResponse.getCode() + ":" + baseResponse.getMessage());
                        //orderInvestService.afterInvestFailed(orderInvestBO.getOrderInvestId(),orderInvestBO.getOrderNo(),baseResponse.getCode(),baseResponse.getMessage());
                        continue;
                    }

                    orderInvestService.syncInvestOrder(orderInvestBO, baseResponse.getData());

                } catch (Exception e) {
                    log.error("同步用户：{},订单：{}异常", orderInvestBO.getUserId(), orderInvestBO.getMerchantNumber(), e);
                    continue;
                }
                //latch.countDown();

            }
            //latch.await();
        }

    }

    @Override
    public YfBuyTransactionDateResponse getBuyTransactionAndExpectedConfirmedDate(String fundCodes) throws Exception {
        YfBuyTransactionDateRequest request = new YfBuyTransactionDateRequest();
        request.setFundCodes(fundCodes);
        BaseResponse<YfBuyTransactionDateResponse> yfResult = (BaseResponse<YfBuyTransactionDateResponse>) thirdOrderInvestService.getBuyTransactionDateAndExpectedConfirmedDate(request);
        return yfResult.getData();
    }

    /**
     * 根据购买金额和组合代码估算相关的申/认购费/费率/折扣费率
     *
     * @param portfolioCode    组合代码
     * @param investmentAmount 买入金额
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public YfPurchasePortfolioTransactionFeeResponse getOrderInvestFee(String portfolioCode, BigDecimal investmentAmount) throws Exception {
        YfPurchasePortfolioTransactionFeeRequest request = new YfPurchasePortfolioTransactionFeeRequest();
        request.setInvestmentAmount(investmentAmount.setScale(2, RoundingMode.HALF_DOWN).toString());
        request.setPortfolioCode(portfolioCode);
        BaseResponse<YfPurchasePortfolioTransactionFeeResponse> yfResult = (BaseResponse<YfPurchasePortfolioTransactionFeeResponse>) thirdOrderInvestService.getPurchasePortfolioTransactionFee(request);
        return yfResult.getData();
    }

    @Override
    public void autoInvest(AutoInvestRequest request) throws Exception {

        log.info("自动定投：{}", request);

        String thirdPlanId = request.getThirdPlanId();
        FundChannelEnum channel = request.getChannel();
        String merchantNumber = request.getMerchantNumber();

        //校验订单是否已经存在
        OrderInvestDO investDO = orderInvestService.queryByMerchantNumber(merchantNumber, channel);
        //TODO
        if (!Objects.isNull(investDO)) {
            log.error("渠道:{}计划:{}定投订单:{}已经存在", channel, thirdPlanId, merchantNumber);
            return;
        }

//        Preconditions.checkState(Objects.isNull(investDO),"渠道:%s计划:%s定投订单:%s已经存在",thirdPlanId,merchantNumber,channel);

        //根据计划ID和渠道查询定投计划
        PlanInfoDO planInfoDO = planService.queryPlanByThirdPlanId(thirdPlanId, channel);

        Preconditions.checkNotNull(planInfoDO, "查询计划渠道%s计划%s失败", thirdPlanId, channel);

        String userId = planInfoDO.getUserId();
        Long planInfoId = planInfoDO.getPlanInfoId();

        //查询用户基金账户
        BaseFundRequest req = new BaseFundRequest();
        req.setUserId(userId);
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);

        Preconditions.checkState(!Objects.isNull(fundUserResponse) && fundUserResponse.isSuccess(), "查询用户%s基金账户失败", userId);

        String accountNumber = fundUserResponse.getData().getAccountNumber();


        //先查询订单
        //根据订单号查询订单详情
        SyncOrderRequest syncOrderRequest = new SyncOrderRequest(accountNumber, merchantNumber);

        log.info("自动定投，查询订单：{}", syncOrderRequest);

        BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                fundPortfolioService.syncOrder(syncOrderRequest);

        Preconditions.checkNotNull(baseResponse, "查询订单:%s无响应", merchantNumber);

        Preconditions.checkState(baseResponse.success(), "查询订单:%s响应失败:%s", merchantNumber, baseResponse.getMessage());

        Preconditions.checkState(!Objects.isNull(baseResponse.getData()) && CollectionUtils.isNotEmpty(baseResponse.getData().getData()),
                "查询订单:%s响应无数据", merchantNumber);

        //订单响应正常，需要初始化订单信息

        //查询计划持仓模型
        List<PlanPortfolioModel> planPortfolioModelList = planService.queryUserPlanPortfolioModel(userId, planInfoId);

        Preconditions.checkState(CollectionUtils.isNotEmpty(planPortfolioModelList), "查询用户:%s计划:%持仓组合失败", userId, planInfoId);
        PlanPortfolioModel planPortfolioModel = planPortfolioModelList.get(0);

        //初始化投资订单
        OrderInvestDO orderInvestDO = new OrderInvestDO();
        //用户id
        orderInvestDO.setUserId(planInfoDO.getUserId());
        //投资金额 TODO 暂时获取不到
        orderInvestDO.setTransactionAmount(BigDecimal.ZERO);
        //渠道
        orderInvestDO.setChannel(channel.getChannel());
        //第三方持仓id 暂无
        orderInvestDO.setThirdPortfolioId(planPortfolioModel.getThirdPortfolioId());
        //组合代码
        orderInvestDO.setPortfolioCode(planPortfolioModel.getThirdPortfolioCode());
        //投资类型-手动买入
        orderInvestDO.setInvestType(InvestTypeEnum.AUTO_INVEST.getType());
        //第三方账户
        orderInvestDO.setAccountNumber(accountNumber);
        //流水号
        orderInvestDO.setMerchantNumber(merchantNumber);

        //遍历子订单，保存订单

        orderInvestService.afterAutoInvest(planInfoDO, orderInvestDO, baseResponse.getData());
    }

    @Override
    public void syncInvestOrder(String merchantNumber, FundChannelEnum channel) throws Exception {

        log.info("同步投资订单：{}", merchantNumber);

        //根据订单号查询投资订单
        OrderInvestDO orderInvestDO = orderInvestService.queryByMerchantNumber(merchantNumber, channel);

        Preconditions.checkNotNull(orderInvestDO, "根据渠道:%s订单号:%查询订单失败", channel, merchantNumber);

        String accountNumber = orderInvestDO.getAccountNumber();

        //查询订单详情
        SyncOrderRequest syncOrderRequest = new SyncOrderRequest(accountNumber,
                merchantNumber);
        BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                fundPortfolioService.syncOrder(syncOrderRequest);

        Preconditions.checkNotNull(baseResponse, "查询账户:%s订单:%s无响应", accountNumber, merchantNumber);

        Preconditions.checkState(baseResponse.success(), "查询账户:%s订单:%s响应失败:%s", accountNumber, merchantNumber, baseResponse.getCode() + ":" + baseResponse.getMessage());

        OrderInvestBO orderInvestBO = new OrderInvestBO();
        orderInvestBO.setUserId(orderInvestDO.getUserId());
        orderInvestBO.setAccountNumber(accountNumber);
        orderInvestBO.setChannel(channel.getChannel());
        orderInvestBO.setMerchantNumber(merchantNumber);
        orderInvestBO.setOrderInvestId(orderInvestDO.getOrderInvestId());
        orderInvestBO.setOrderNo(orderInvestDO.getOrderNo());
        orderInvestBO.setPortfolioCode(orderInvestDO.getPortfolioCode());

        orderInvestService.syncInvestOrder(orderInvestBO, baseResponse.getData());
    }

    @Override
    public Boolean isInvested(String userId) {
        return orderInvestService.countUserInvestOrder(userId,InvestTypeEnum.MANUAL)>0;
    }

}
