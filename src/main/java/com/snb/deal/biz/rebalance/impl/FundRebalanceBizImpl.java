package com.snb.deal.biz.rebalance.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jianlc.tc.guid.GuidCreater;
import com.jianlc.tc.jtracker.client.TraceContext;
import com.jianlc.tc.jtracker.common.Span;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.plan.portfolio.PortfolioAccountRequest;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.plan.PortfolioAccountBO;
import com.snb.deal.bo.plan.PortfolioAccountDetailBO;
import com.snb.deal.bo.rebalance.*;
import com.snb.deal.entity.order.OrderRebalanceAuthDO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import com.snb.deal.entity.order.OrderRebalanceDetailDO;
import com.snb.deal.entity.order.OrderRebalanceFailureRecordDO;
import com.snb.deal.entity.plan.PlanPortfolioRelDO;
import com.snb.deal.enums.*;
import com.snb.deal.mapper.order.OrderInfoMapper;
import com.snb.deal.mapper.order.OrderRebalanceAuthMapper;
import com.snb.deal.mapper.order.OrderRebalanceDetailMapper;
import com.snb.deal.mapper.plan.PlanPortfolioRelMapper;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderRebalanceFailureRecordService;
import com.snb.deal.service.order.OrderRebalanceService;
import com.snb.deal.service.plan.PlanService;
import com.snb.deal.thread.CreateRebalanceThread;
import com.snb.deal.thread.SendRebalanceThread;
import com.snb.fund.api.dto.mainmodel.FundMainModelDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailRequest;
import com.snb.fund.api.dto.mainmodel.FundMainModelRequest;
import com.snb.fund.api.enums.FundMainModelStatusEnums;
import com.snb.fund.api.remote.FundMainModelRemote;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import com.snb.third.yifeng.dto.order.SyncOrderResponse;
import com.snb.third.yifeng.dto.order.rebalance.*;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FundRebalanceBizImpl implements FundRebalanceBiz {
    @Reference(version = "1.0", check = false)
    FundMainModelRemote fundMainModelRemote;
    @Resource
    PlanPortfolioRelMapper planPortfolioRelMapper;
    @Resource
    OrderRebalanceService rebalanceService;
    @Resource
    FundPortfolioService fundPortfolioService;
    @Reference(version = "1.0", check = false)
    FundUserRemote fundUserRemote;
    @Resource
    OrderRebalanceService orderRebalanceService;
    @Resource
    PlanBiz planBiz;
    @Resource
    OrderRebalanceFailureRecordService orderRebalanceFailureRecordService;
    @Resource
    OrderRebalanceDetailMapper orderRebalanceDetailMapper;
    @Resource
    OrderInfoMapper orderInfoMapper;
    @Resource
    OrderRebalanceAuthMapper orderRebalanceAuthMapper;
    @Resource
    FlowNumberService flowNumberService;
    @Resource
    GuidCreater guidCreater;
    @Resource
    Environment environment;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void createRebalance() {
        // 查询主理人模型信息
        FundMainModelDTO fundMainModelDTO = this.getFundMainModelDTO();
        List<FundMainModelDetailDTO> fundMainModelDetailDTOList = this.getFundMainModelDetail(fundMainModelDTO);

        // 赋值调仓的组合标准信息到redis中
        this.setRebalanceStandar2Redis(fundMainModelDTO, fundMainModelDetailDTOList);

        int startIndex = 0;
        int count = environment.getProperty("rebalance.startPageCount", Integer.class, 200);
        int threadNum = environment.getProperty("rebalance.threadNum", Integer.class, 10);

        List<Callable<Boolean>> rebalanceList = Lists.newArrayList();
        ExecutorService pool = Executors.newFixedThreadPool(threadNum);

        // 查询主理人模型下的用户-分页获取
        List<PlanPortfolioRelDO> planPortfolioRelList =
                planPortfolioRelMapper.getBatchRebalanceUserList(fundMainModelDTO.getFundMainModelId(), startIndex, count);

        try {
            final Span span = TraceContext.getCurrentSpan();
            while (!CollectionUtils.isEmpty(planPortfolioRelList)) {
                log.info("调仓任务：查询调仓用户数量={}", planPortfolioRelList.size());
                for (final PlanPortfolioRelDO planPortfolioRel : planPortfolioRelList) {
                    OrderRebalanceCreateBO rebalanceInfoBO = new OrderRebalanceCreateBO();
                    rebalanceInfoBO.setFundMainModelId(planPortfolioRel.getMainModelId());
                    rebalanceInfoBO.setUserId(planPortfolioRel.getUserId());
                    rebalanceInfoBO.setPlanPortfolioRelId(planPortfolioRel.getPlanPortfolioRelId());
                    rebalanceInfoBO.setThirdPortfolioId(planPortfolioRel.getThirdPortfolioId());
                    rebalanceInfoBO.setThirdPortfolioCode(fundMainModelDTO.getThirdPortfolioCode());
                    rebalanceInfoBO.setChannel(planPortfolioRel.getChannel());
                    rebalanceInfoBO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
                    rebalanceInfoBO.setPlanId(planPortfolioRel.getPlanId());

                    // 加入线程线程池
                    rebalanceList.add(new CreateRebalanceThread(rebalanceInfoBO, this, span));
                }

                if (!CollectionUtils.isEmpty(rebalanceList)) {
                    pool.invokeAll(rebalanceList);
                }

                rebalanceList.clear();
                startIndex += count;
                planPortfolioRelList = planPortfolioRelMapper.getBatchRebalanceUserList(fundMainModelDTO.getFundMainModelId(), startIndex, count);
            }
        } catch (Exception e) {
            log.error("调仓任务 {}", Thread.currentThread(), e);
        } finally {
            try {
                pool.shutdown();

                String thirdPortfolioCode = fundMainModelDTO.getThirdPortfolioCode();
                // 删除redis中的缓存
                stringRedisTemplate.delete(stringRedisTemplate.keys(thirdPortfolioCode + ":*"));
                log.error("调仓任务-删除redis中的缓存:thirdPortfolioCode:*,thirdPortfolioCode={}", thirdPortfolioCode);

            } catch (Exception e) {
                log.error("调仓任务线程池关闭出现异常", e);
            }
        }
    }

    @Override
    public void sendRebalance() {
        int startIndex = 0;
        int count = environment.getProperty("rebalance.startPageCount", Integer.class, 200);
        int threadNum = environment.getProperty("rebalance.threadNum", Integer.class, 10);

        List<Callable<Boolean>> rebalanceList = Lists.newArrayList();
        ExecutorService pool = Executors.newFixedThreadPool(threadNum);

        List<OrderRebalanceSendBO> orderRebalanceSendBOList =
                orderRebalanceService.querySendOrderRebalanceList(OrderRebalanceAuthStatusEnum.COMPLETE.getCode(), startIndex, count);
        try {
            int num = 0;
            while (!CollectionUtils.isEmpty(orderRebalanceSendBOList)) {
                num = num + orderRebalanceSendBOList.size();
                for (OrderRebalanceSendBO orderRebalanceSendBO : orderRebalanceSendBOList) {
                    // 加入线程线程池
                     rebalanceList.add(new SendRebalanceThread(orderRebalanceSendBO, this));
                }
                if (!CollectionUtils.isEmpty(rebalanceList)) {
                    pool.invokeAll(rebalanceList);
                }
                startIndex += count;
                rebalanceList.clear();
                orderRebalanceSendBOList = orderRebalanceService
                        .querySendOrderRebalanceList(OrderRebalanceAuthStatusEnum.COMPLETE.getCode(), startIndex, count);
            }
            log.info("{}调仓任务：查询发送调仓用户数量={}", Thread.currentThread(), orderRebalanceSendBOList.size());
        } catch (Exception e) {
            log.error("发送调仓任务 {}", Thread.currentThread(), e);
        } finally {
            try {
                pool.shutdown();
            } catch (Exception e) {
                log.error("调仓任务线程池关闭出现异常", e);
            }
        }
    }

    @Override
    public void singleSendRebalance(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception {
        // 更新发送时间
        rebalanceService.updateOrderSendTime(orderRebalanceSendBO);
        // 发送调仓请求到奕丰
        BaseResponse<YfRebalancePortfolioResponse> baseResponse = this.sendRebalancePositions2yifeng(orderRebalanceSendBO);
        log.info("{}调仓交易 ，奕丰响应：{}", Thread.currentThread(), JSONObject.toJSONString(baseResponse));

        if (baseResponse.success()) {
            YfRebalancePortfolioResponse rebalancePortfolioResponse = baseResponse.getData();
            // 返回的调仓明细列表判断
            if (CollectionUtils.isEmpty(rebalancePortfolioResponse.getRebalanceResult())) {
                log.error("{}发送调仓请求到奕丰处理,调仓明细列表空异常" + JSONObject.toJSONString(baseResponse), Thread.currentThread());
                throw new RuntimeException("发送调仓请求到奕丰处理,调仓明细列表空异常" + JSONObject.toJSONString(baseResponse));
            }
            // 调仓交易发送后处理
            rebalanceService.rebalanceAfterDeal(rebalancePortfolioResponse, orderRebalanceSendBO);
        } else {
            String respCode = baseResponse.getCode();
            if ("108".equals(respCode) || "207".equals(respCode) || "208".equals(respCode)
                    || "209".equals(respCode) || "414".equals(respCode)) {
                rebalanceService.rebalanceStartFailure(orderRebalanceSendBO);
            }
            OrderRebalanceFailureRecordDO orderRebalanceFailureRecordDO = new OrderRebalanceFailureRecordDO();
            orderRebalanceFailureRecordDO.setUserId(orderRebalanceSendBO.getUserId());
            orderRebalanceFailureRecordDO.setOrderRebalanceFailureId(guidCreater.getUniqueID());
            orderRebalanceFailureRecordDO.setMerchantNumber(orderRebalanceSendBO.getMerchantNumber());
            orderRebalanceFailureRecordDO.setRebalanceType(OrderRebalanceFailureBusiTypeEnum.SEND_REBALANCE_ORDER.getCode());
            orderRebalanceFailureRecordDO.setRequestInfo(JSONObject.toJSONString(orderRebalanceSendBO));
            orderRebalanceFailureRecordDO.setResponseInfo(JSONObject.toJSONString(baseResponse));
            orderRebalanceFailureRecordDO.setErrMessage(baseResponse.getMessage());
            orderRebalanceFailureRecordDO.setErrCode(baseResponse.getCode());
            orderRebalanceFailureRecordDO.setChannel(orderRebalanceSendBO.getChannel());
            orderRebalanceFailureRecordService.insert(orderRebalanceFailureRecordDO);
        }
    }

    @Override
    public void syncOrderRebalanceBiz(OrderRebalanceAsyncBO orderRebalanceAsyncBO,
                                      BaseResponse<SyncOrderListResponse> baseResponse) throws Exception {
        SyncOrderListResponse syncOrderListResponse = baseResponse.getData();
        // 返回的订单列表
        List<SyncOrderResponse> syncOrderResponseList = syncOrderListResponse.getData();
        if (!CollectionUtils.isEmpty(syncOrderResponseList)) {

            OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
            orderRebalanceConditionBO.setUserId(orderRebalanceAsyncBO.getUserId());
            orderRebalanceConditionBO.setOrderRebalanceId(orderRebalanceAsyncBO.getOrderRebanlanceId());

            List<OrderRebalanceDetailDO> addList = Lists.newArrayList();
            List<OrderRebalanceDetailDO> updateList = Lists.newArrayList();

            OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
            orderRebalanceDO.setOrderRebalanceId(orderRebalanceAsyncBO.getOrderRebanlanceId());
            orderRebalanceDO.setTransactionCharge(BigDecimal.ZERO);
            orderRebalanceDO.setUserId(orderRebalanceAsyncBO.getUserId());
            orderRebalanceDO.setOrderNo(orderRebalanceAsyncBO.getOrderNo());

            for (SyncOrderResponse sor : syncOrderResponseList) {
                // 解析交易状态
                TransactionStatusEnum transactionStatusEnum =
                        TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, sor.getTransactionStatus());
                // 解析交易类型
                OrderRebalanceTransactionTypeEnum rebalanceTransactionTypeEnum =
                        OrderRebalanceTransactionTypeEnum.getRebalanceTransactionTypeEnum(FundChannelEnum.YIFENG, sor.getTransactionType());

                OrderRebalanceDetailDO orderRebalanceDetailDO = new OrderRebalanceDetailDO();
                orderRebalanceDetailDO.setUserId(orderRebalanceAsyncBO.getUserId());
                orderRebalanceDetailDO.setOrderRebalanceId(orderRebalanceAsyncBO.getOrderRebanlanceId());
                orderRebalanceDetailDO.setContractNumber(sor.getContractNumber());
                orderRebalanceDetailDO.setFundCode(sor.getFundCode());
                orderRebalanceDetailDO.setFundName(sor.getFundName());
                orderRebalanceDetailDO.setInvestAmount(new BigDecimal(sor.getTransactionAmount() == null ? "0.00" : sor.getTransactionAmount()));
                orderRebalanceDetailDO.setOrderDate(DateTimeUtil.parseDate(sor.getOrderDate()));
                orderRebalanceDetailDO.setReason(sor.getReason());
                orderRebalanceDetailDO.setRedemptionUnit(new BigDecimal(sor.getTransactionUnit() == null ? "0.00" : sor.getTransactionUnit()));
                orderRebalanceDetailDO.setSettlementDate(sor.getSettlementDate() == null ? null : DateTimeUtil.parseDate(sor.getSettlementDate()));

                if (rebalanceTransactionTypeEnum.getCode().equals(OrderRebalanceTransactionTypeEnum.ORDERREBALANCE_INVEST.getCode())) {
                    int day1 = DateTimeUtil.getDaysBetweenDate(
                            Long.parseLong(sor.getPricedDate()), Long.parseLong(sor.getTransactionDate()));
                    // 基金申购确认日差
                    orderRebalanceDetailDO.setTransactionCfmLagDay(Math.abs(day1));
                }

                if (rebalanceTransactionTypeEnum.getCode().equals(OrderRebalanceTransactionTypeEnum.ORDERREBALANCE_REDEEM.getCode())) {
                    int day2 = DateTimeUtil.getDaysBetweenDate(
                            Long.parseLong(sor.getSettlementDate()), Long.parseLong(sor.getTransactionDate()));
                    // 基金赎回清算日差
                    orderRebalanceDetailDO.setTransactionSettleLagDay(Math.abs(day2));
                }

                orderRebalanceDetailDO.setTransactionDate(sor.getTransactionDate() == null ? null : DateTimeUtil.parseDate(sor.getTransactionDate()));

                orderRebalanceDetailDO.setTransactionStatus(transactionStatusEnum.getCode());
                orderRebalanceDetailDO.setTransactionType(rebalanceTransactionTypeEnum.getCode());
                orderRebalanceDetailDO.setTransactionRate(new BigDecimal(sor.getTransactionRate() == null ? "0.00" : sor.getTransactionRate()));
                orderRebalanceDetailDO.setDiscountRate(new BigDecimal(sor.getDiscountRate() == null ? "0.00" : sor.getDiscountRate()));
                orderRebalanceDetailDO.setTransactionCharge(new BigDecimal(sor.getTransactionCharge() == null ? "0.00" : sor.getTransactionCharge()));
                orderRebalanceDetailDO.setBankCode(sor.getBankCode());
                orderRebalanceDetailDO.setBankNumber(sor.getBankNumber());
                orderRebalanceDetailDO.setAccountNumber(sor.getAccountNumber());
                orderRebalanceDetailDO.setCancelEnable(sor.getCancelEnable());
                orderRebalanceDetailDO.setCanceledDate(sor.getCanceledDate() == null ? null : DateTimeUtil.parseDate(sor.getCanceledDate()));
                orderRebalanceDetailDO.setCompletedDate(sor.getCompletedDate() == null ? null : DateTimeUtil.parseDate(sor.getCompletedDate()));
                orderRebalanceDetailDO.setDiscountTransactionCharge(new BigDecimal(sor.getDiscountTransactionCharge() == null ? "0.00" : sor.getDiscountTransactionCharge()));
                orderRebalanceDetailDO.setExpectedConfirmedDate(sor.getExpectedConfirmedDate() == null ? null : DateTimeUtil.parseDate(sor.getExpectedConfirmedDate()));
                orderRebalanceDetailDO.setInvestorPayId(sor.getPayMethod() == null ? null : Integer.parseInt(sor.getPayMethod()));
                orderRebalanceDetailDO.setPricedDate(sor.getPricedDate() == null ? null : DateTimeUtil.parseDate(sor.getPricedDate()));
                orderRebalanceDetailDO.setTransactionUnit(new BigDecimal(sor.getTransactionUnit() == null ? "0.00" : sor.getTransactionUnit()));
                orderRebalanceDetailDO.setVoidDate(sor.getVoidDate() == null ? null : DateTimeUtil.parseDate(sor.getVoidDate()));
                // 查询子订单是否存在
                orderRebalanceConditionBO.setTransactionType(rebalanceTransactionTypeEnum.getCode());
                Long count = orderRebalanceDetailMapper.queryRebalanceCountByCondition(orderRebalanceConditionBO);
                // 新增
                if (count == 0L) {
                    orderRebalanceDetailDO.setOrderRebalanceDetailId(guidCreater.getUniqueID());
                    addList.add(orderRebalanceDetailDO);
                } else {
                    updateList.add(orderRebalanceDetailDO);
                }
                /*orderRebalanceDO.setTransactionCharge(orderRebalanceDO.getTransactionCharge()
                        .add(orderRebalanceDetailDO.getTransactionCharge()));*/
            }
            // 订单同步数据处理
            rebalanceService.syncOrderRebalance(addList, updateList, orderRebalanceDO);
        }
    }

    @Override
    public boolean rebalanceOrderSync() {
//        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
//        orderRebalanceConditionBO.setChannel(FundChannelEnum.YIFENG.getChannel());
//        orderRebalanceConditionBO.setBusinessCode(OrderBusinessEnum.REBALANCE.getCode());
//        orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
        Integer pagetIndex = 0;
        Integer pageSize = environment.getProperty("rebalance.orderSyncPageSize", Integer.class, 200);
        //1.查询异步同步调仓订单请求集合
        List<OrderRebalanceAsyncBO> orderRebalanceAsyncBOList = orderRebalanceService.queryOrderRebalanceAsync(
                FundChannelEnum.YIFENG.getChannel(), OrderBusinessEnum.REBALANCE.getCode(), OrderStatusEnum.PROCESS.getCode(),
                pagetIndex, pageSize);
        Boolean isAllSuccess = true;
        while (!CollectionUtils.isEmpty(orderRebalanceAsyncBOList)) {
            for (OrderRebalanceAsyncBO orderRebalanceAsyncBO : orderRebalanceAsyncBOList) {
                try {
                    log.info("调仓订单同步开始,orderRebalanceAsyncBO={}", JSONObject.toJSONString(orderRebalanceAsyncBO));
                    // 调仓
                    this.orderRebalanceAsync(orderRebalanceAsyncBO);
                } catch (Exception e) {
                    isAllSuccess = false;
                    log.error("调仓订单同步异常,orderRebalanceAsyncBO={}", JSONObject.toJSONString(orderRebalanceAsyncBO));
                }
            }
            pagetIndex += pageSize;
            orderRebalanceAsyncBOList = orderRebalanceService.queryOrderRebalanceAsync(
                    FundChannelEnum.YIFENG.getChannel(), OrderBusinessEnum.REBALANCE.getCode(), OrderStatusEnum.PROCESS.getCode(),
                    pagetIndex, pageSize);
        }
        return isAllSuccess;
    }

    @Override
    public void orderRebalanceAsync(OrderRebalanceAsyncBO orderRebalanceAsyncBO) throws Exception {
        if (orderRebalanceAsyncBO == null) {
            log.info("调仓信息异常，无需同步！orderRebalanceAsyncBO=null");
            return;
        }
        OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
        orderRebalanceAuthConditionBO.setOrderRebalanceId(orderRebalanceAsyncBO.getOrderRebanlanceId());
        orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        OrderRebalanceAuthDO orderRebalanceAuthDO = orderRebalanceAuthMapper.queryOrderRebalanceAuth(orderRebalanceAuthConditionBO);
        if (orderRebalanceAuthDO != null) {
            log.info("用户{}的调仓订单{}同步，用户调仓订单未授权，无需同步！OrderRebalanceAuthConditionBO={}", orderRebalanceAsyncBO.getUserId(),
                    orderRebalanceAsyncBO.getOrderRebanlanceId(), JSONObject.toJSONString(orderRebalanceAuthConditionBO));
            return;
        }
        //发送查询订单请求
        SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRebalanceAsyncBO.getAccountNumber(),
                orderRebalanceAsyncBO.getMerchantNumber());
        log.info("{}调仓订单同步任务请求：" + JSONObject.toJSONString(syncOrderRequest), Thread.currentThread());

        BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                fundPortfolioService.syncOrder(syncOrderRequest);


        log.info("{}调仓订单同步任务应答：" + JSONObject.toJSONString(baseResponse), Thread.currentThread());
        if (baseResponse.success()) {
            // 订单同步处理
            this.syncOrderRebalanceBiz(orderRebalanceAsyncBO, baseResponse);
        } else {
            log.error("{}调仓订单同步任务处理异常：" + JSONObject.toJSONString(syncOrderRequest), Thread.currentThread());
        }
    }


    /**
     * 调仓业务处理-调仓前处理-[核心流程控制]
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    @Override
    public void singleCreateRebalance(OrderRebalanceCreateBO orderRebalanceCreateBO) throws Exception {
        // 1. 检查用户是否满足调仓前置条件
        if (!prepositionCheckUserRebalance(orderRebalanceCreateBO)) {
            return;
        }

        GetUserFundAccountInfoResponse userFundAccountInfoResponse = this.getGetUserFundAccountInfo(orderRebalanceCreateBO);
        if (userFundAccountInfoResponse == null) {
            log.info("{}调仓创建任务，用户{}获取用户基金账户信息为空,退出调仓！", Thread.currentThread(), orderRebalanceCreateBO.getUserId());
            return;
        }
        // 2.1 获取三方账户信息
        orderRebalanceCreateBO.setThirdAccountNumber(userFundAccountInfoResponse.getAccountNumber());

        // 2.1 获取组合持有基金账户明细信息
        List<PortfolioAccountDetailBO> portfolioAccountDetailBOList = this.getPortfolioAccountDetailBOList(orderRebalanceCreateBO);
        if (CollectionUtils.isEmpty(portfolioAccountDetailBOList)) {
            log.info("{}调仓创建任务，用户{}基金明细持仓明细空,退出调仓处理！,PortfolioAccountDetailBOList={}", Thread.currentThread(),
                    orderRebalanceCreateBO.getUserId(), JSONObject.toJSONString(orderRebalanceCreateBO));
            return;
        }

        // 2.2 获取用户持仓总可用金额
        BigDecimal userPortfolioTotalAvailableAmount = this.getUserPortfolioTotalAvailableAmount(portfolioAccountDetailBOList);
        if (BigDecimal.ZERO.compareTo(userPortfolioTotalAvailableAmount) == 0) {
            log.info("{}调仓创建任务，用户{}可用金额等于=0,退出调仓处理！", Thread.currentThread(), orderRebalanceCreateBO.getUserId());
            return;
        }

        // 2.3 判断用户是否需要调仓
        boolean needRebalance = this.checkUserRebalance(portfolioAccountDetailBOList, orderRebalanceCreateBO, userPortfolioTotalAvailableAmount);

        // 3.1 处理需要调仓的情况
        if (needRebalance) {
            // 请求奕丰获取调仓概要信息
            BaseResponse<YfRebalanceSummaryResponse> baseResponse = this.sendRebalanceSummary2yifeng(orderRebalanceCreateBO);
            if (baseResponse == null) {
                log.info("{}调仓创建任务，用户{}发送奕丰概要信息接口无响应，退出调仓处理！请求参数={}", Thread.currentThread(), orderRebalanceCreateBO.getUserId(),
                        JSONObject.toJSONString(orderRebalanceCreateBO));
                return;
            }

            log.info("{}调仓创建任务：用户{}请求奕丰获取调仓概要信息==>{}", Thread.currentThread(), orderRebalanceCreateBO.getUserId(),
                    JSONObject.toJSONString(baseResponse));
            if (baseResponse.success()) {
                // 调仓交易前处理
                YfRebalanceSummaryResponse yfRebalanceSummaryResponse = baseResponse.getData();
                List<YfRebalanceDetailSummary> yfRebalanceDetailSummaryList = yfRebalanceSummaryResponse.getRebalanceDetails();
                List<YfRebalanceFeeDetailSummary> yfRebalanceFeeDetailSummaryList = yfRebalanceSummaryResponse.getFeeDetails();

                if (CollectionUtils.isEmpty(yfRebalanceDetailSummaryList)) {
                    log.info("{}调仓创建任务，用户{}调仓概要预计信息空，退出调仓！rebalanceDisableReson={}", Thread.currentThread(),
                            orderRebalanceCreateBO.getUserId(), yfRebalanceSummaryResponse.getRebalanceDisableReson());
                    return;
                }

                if (CollectionUtils.isEmpty(yfRebalanceFeeDetailSummaryList)) {
                    log.info("{}调仓创建任务，用户{}调仓概要费用信息空，退出调仓！rebalanceDisableReson={}", Thread.currentThread(),
                            orderRebalanceCreateBO.getUserId(), yfRebalanceSummaryResponse.getRebalanceDisableReson());
                    return;
                }

                // 校验调仓费用比例是否超出限制范围
                if (checkRebalanceStandarFeeRate(yfRebalanceSummaryResponse, userPortfolioTotalAvailableAmount, orderRebalanceCreateBO)) {
                    log.info("{}调仓创建任务，用户{}调仓费用超出了限制范围,退出调仓任务!", Thread.currentThread(), orderRebalanceCreateBO.getUserId());
                    return;
                }
                // 调仓前处理-数据持久化
                rebalanceService.rebalanceBeforeDeal(orderRebalanceCreateBO, yfRebalanceSummaryResponse);
            } else {
                // 调仓获取概要失败处理
                this.createRebalanceFailRecord(orderRebalanceCreateBO, baseResponse);
            }
        }
        // 3.2 处理不需要调仓的情况
        if (!needRebalance) {
            OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
            orderRebalanceDO.setUserId(orderRebalanceCreateBO.getUserId());
            orderRebalanceDO.setChannel(orderRebalanceCreateBO.getChannel());
            // 调仓之前相关订单作废
            rebalanceService.rebalanceAbandon(orderRebalanceDO);
        }
    }

    /**
     * 调仓获取概要失败处理
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createRebalanceFailRecord(OrderRebalanceCreateBO orderRebalanceCreateBO, BaseResponse<YfRebalanceSummaryResponse> baseResponse) {
        OrderRebalanceFailureRecordDO orderRebalanceFailureRecordDO = new OrderRebalanceFailureRecordDO();
        orderRebalanceFailureRecordDO.setUserId(orderRebalanceCreateBO.getUserId());
        orderRebalanceFailureRecordDO.setOrderRebalanceFailureId(guidCreater.getUniqueID());
        orderRebalanceFailureRecordDO.setMerchantNumber(orderRebalanceCreateBO.getMerchantNumber());
        orderRebalanceFailureRecordDO.setRebalanceType(OrderRebalanceFailureBusiTypeEnum.SYNC_REBALANCE_SUMMARY.getCode());
        orderRebalanceFailureRecordDO.setRequestInfo(JSONObject.toJSONString(orderRebalanceCreateBO));
        orderRebalanceFailureRecordDO.setResponseInfo(JSONObject.toJSONString(baseResponse));
        orderRebalanceFailureRecordDO.setErrMessage(baseResponse.getMessage());
        orderRebalanceFailureRecordDO.setErrCode(baseResponse.getCode());
        //临时处理，奕丰返回的code为msg，msg为空
        String code = baseResponse.getCode();
        if (!StringUtils.isEmpty(code) && !"null".equalsIgnoreCase(code)) {
            if (code.length() > 10) {
                //非法code
                orderRebalanceFailureRecordDO.setErrCode("-1");
                orderRebalanceFailureRecordDO.setErrMessage(code);
            }
        }
        orderRebalanceFailureRecordDO.setChannel(orderRebalanceCreateBO.getChannel());
        orderRebalanceFailureRecordService.insert(orderRebalanceFailureRecordDO);
    }

    /**
     * 校验调仓费用比例是否超出限制范围
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private boolean checkRebalanceStandarFeeRate(YfRebalanceSummaryResponse yfRebalanceSummaryResponse, BigDecimal currentTotalValue,
                                                 OrderRebalanceCreateBO orderRebalanceCreateBO) {
        // 交易手续总费用
        BigDecimal totalFee = new BigDecimal(yfRebalanceSummaryResponse.getTotalRebalanceFee() == null ? "0.00"
                : yfRebalanceSummaryResponse.getTotalRebalanceFee());
        // 调仓费用比例 = 总费用/现值资产总额
        BigDecimal feeRate = totalFee.divide(currentTotalValue, 4, BigDecimal.ROUND_HALF_DOWN);
        // 调仓费用比例标准
        BigDecimal rebalanceStandarFeeRate = new BigDecimal(environment.getProperty("rebalance.standarFeeRate"));

        log.info("{}调仓任务，校验调仓费用比例:用户{}调仓费比例={},控制费用比例={},计算参数totalFee={},现值总资产={}",
                Thread.currentThread(), orderRebalanceCreateBO.getUserId(),
                feeRate, rebalanceStandarFeeRate, totalFee, currentTotalValue);

        // 判断调仓费用是否超出限制范围
        if (feeRate.compareTo(rebalanceStandarFeeRate) > 0) {
            return true;
        }
        return false;
    }

    /**
     * 查询组合账户
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private List<PortfolioAccountDetailBO> getPortfolioAccountDetailBOList(OrderRebalanceCreateBO orderRebalanceCreateBO) throws Exception {
        PortfolioAccountBO portfolioAccountBO = planBiz.queryPortfolioAccountFromThird(orderRebalanceCreateBO.getUserId(), orderRebalanceCreateBO.getPlanId());
        if (portfolioAccountBO == null) {
            return null;
        }
        return portfolioAccountBO.getPortfolioAccountDetailBOList();
    }

    /**
     * 调仓获取用户基金账户信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private GetUserFundAccountInfoResponse getGetUserFundAccountInfo(OrderRebalanceCreateBO orderRebalanceCreateBO) throws Exception {
        int channel = orderRebalanceCreateBO.getChannel();
        BaseFundRequest request = new BaseFundRequest();
        request.setFundPlatform(FundChannelEnum.getByChannel(channel));
        request.setUserId(orderRebalanceCreateBO.getUserId());
        APIResponse<GetUserFundAccountInfoResponse> userAccountAPIResponse = fundUserRemote.getUserFundAccountInfo(request);
        return userAccountAPIResponse.getData();
    }

    /**
     * 请求奕丰获取调仓概要信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-19
     */
    private BaseResponse<YfRebalanceSummaryResponse> sendRebalanceSummary2yifeng(OrderRebalanceCreateBO orderRebalanceCreateBO) throws Exception {
        YfRebalanceSummaryRequest yfRebalanceSummaryRequest = new YfRebalanceSummaryRequest();
        yfRebalanceSummaryRequest.setAccountNumber(orderRebalanceCreateBO.getThirdAccountNumber());
        yfRebalanceSummaryRequest.setPortfolioId(orderRebalanceCreateBO.getThirdPortfolioId());
        log.info("{}调仓交易-概要信息,发送请求：{}", Thread.currentThread(), JSONObject.toJSONString(yfRebalanceSummaryRequest));
        return (BaseResponse<YfRebalanceSummaryResponse>) fundPortfolioService.rebalanceTransactionSummary(yfRebalanceSummaryRequest);
    }

    /**
     * 调仓交易
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    private BaseResponse<YfRebalancePortfolioResponse> sendRebalancePositions2yifeng(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception {
        // 构建调仓请求数据
        YfRebalancePortfolioRequest rpr = new YfRebalancePortfolioRequest();
        rpr.setAccountNumber(orderRebalanceSendBO.getThirdAccountNumber());
        rpr.setMerchantNumber(orderRebalanceSendBO.getMerchantNumber());
        rpr.setPortfolioId(orderRebalanceSendBO.getThirdPortfolioId());
        rpr.setBuyMode(OrderRebalanceBuyModeEnum.FUNDMODELSCALE.getCode());
        rpr.setRiskConfrimed("1");
        String notifyUrl = environment.getProperty("snb.api.url.domain") + "/" + environment.getProperty("ifast.order.callback.url");
        rpr.setNotifyUrl(notifyUrl);
        log.info("{}调仓交易 ，发送请求：{}", Thread.currentThread(), JSONObject.toJSONString(rpr));
        return (BaseResponse<YfRebalancePortfolioResponse>) fundPortfolioService.rebalancePortfolioTransaction(rpr);

    }

    /**
     * 计算是否需要调仓
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    private boolean checkUserRebalance(List<PortfolioAccountDetailBO> portfolioAccountDetailBOList,
                                       OrderRebalanceCreateBO orderRebalanceCreateBO, BigDecimal userPortfolioTotalAvailableAmount) {
        // 查询主理人模型信息
        FundMainModelDTO fundMainModelDTO = this.getFundMainModelDTO();
        List<FundMainModelDetailDTO> fundMainModelDetailDTOList = this.getFundMainModelDetail(fundMainModelDTO);
        // 若主理人模型下的基金数量和用户持仓基金数量不一致，需要调仓
        if (fundMainModelDetailDTOList.size() != portfolioAccountDetailBOList.size()) {
            return true;
        }

        // 循环用户持有的基金明细判断是否需要调仓
        for (PortfolioAccountDetailBO portfolioAccountDetailBO : portfolioAccountDetailBOList) {
            // 调仓交易的基金可用金额 = 基金的可用份额 * 基金的净值
            BigDecimal portfolioFundAvailableAmount = portfolioAccountDetailBO.getAvailableUnit()
                    .multiply(portfolioAccountDetailBO.getNav());
            // 基金当前占比 = 每只基金的可用金额/用户总的可用金额
            BigDecimal fundCurrentAccounting = portfolioFundAvailableAmount.divide(userPortfolioTotalAvailableAmount, 4, BigDecimal.ROUND_HALF_DOWN)
                    .multiply(new BigDecimal("100"));
            log.info("{}:用户{}基金{}用户可用金额={},({}*{})/{}的占比{}", Thread.currentThread(), orderRebalanceCreateBO.getUserId(),
                    portfolioAccountDetailBO.getFundCode(), userPortfolioTotalAvailableAmount, portfolioAccountDetailBO.getAvailableUnit(),
                    portfolioAccountDetailBO.getNav(), userPortfolioTotalAvailableAmount, fundCurrentAccounting);
            // 校验是否需要调仓-逐一校验
            if (compareThreshold(fundCurrentAccounting, orderRebalanceCreateBO, portfolioAccountDetailBO)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 阈值比较
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    private boolean compareThreshold(BigDecimal fundCurrentAccounting, OrderRebalanceCreateBO orderRebalanceCreateBO
            , PortfolioAccountDetailBO portfolioAccountDetailBO) {
        //1.1 从Redis中获取调仓标准阈值
        String rebalanceStandardThresholdKey = orderRebalanceCreateBO.getThirdPortfolioCode() + ":" + portfolioAccountDetailBO.getFundCode();
        String rebalanceStandardThresholdValue = stringRedisTemplate.opsForValue().get(rebalanceStandardThresholdKey);
        if (StringUtils.isEmpty(rebalanceStandardThresholdValue)) {
            log.info("{}调仓创建任务：用户{}从Redis获取到调仓标准阙值为空", Thread.currentThread(), orderRebalanceCreateBO.getUserId());
            return true;
        }
        BigDecimal rebalanceStandardThreshold = new BigDecimal(rebalanceStandardThresholdValue);
        //1.2 调仓浮动值
        String rebalanceFloatValueString = environment.getProperty("rebalance.floatValue");
        if (StringUtils.isEmpty(rebalanceFloatValueString)) {
            log.error("{}调仓创建任务：用户{}，从配置中心获取到调仓浮动值为空", Thread.currentThread(), orderRebalanceCreateBO.getUserId());
            throw new RuntimeException("从配置中心获取到调仓浮动值为空");
        }
        BigDecimal rebalanceFloatValue = new BigDecimal(rebalanceFloatValueString);

        //2.1 高于阀值比较
        if (fundCurrentAccounting.compareTo(rebalanceStandardThreshold.add(rebalanceFloatValue)) > 0) {
            log.info("{}调仓创建任务：用户{}持有基金{}当前占比{},超出模型标准{}+{}={}，发起调仓！", Thread.currentThread(),
                    orderRebalanceCreateBO.getUserId(), portfolioAccountDetailBO.getFundCode(), fundCurrentAccounting,
                    rebalanceStandardThreshold, rebalanceFloatValue, rebalanceStandardThreshold.add(rebalanceFloatValue));
            return true;
        }
        //2.2 低于阀值比较
        if (fundCurrentAccounting.compareTo(rebalanceStandardThreshold.subtract(rebalanceFloatValue)) < 0) {
            log.info("{}调仓任务：用户{}持有基金{}当前占比{},低于模型标准{}-{}={}，发起调仓！", Thread.currentThread(),
                    orderRebalanceCreateBO.getUserId(), portfolioAccountDetailBO.getFundCode(), fundCurrentAccounting,
                    rebalanceStandardThreshold, rebalanceFloatValue, rebalanceStandardThreshold.subtract(rebalanceFloatValue));
            return true;
        }
        //2.3 介于模型标准之间
        log.info("{}调仓任务：用户{}持有基金{}当前占比={},于{}~{}之间，标准阙值={},浮动值={},无需调仓！",
                Thread.currentThread(), orderRebalanceCreateBO.getUserId(), portfolioAccountDetailBO.getFundCode(),
                fundCurrentAccounting, rebalanceStandardThreshold.subtract(rebalanceFloatValue),
                rebalanceStandardThreshold.add(rebalanceFloatValue), rebalanceStandardThreshold, rebalanceFloatValue);

        return false;
    }

    /**
     * 用户持仓总可用金额 = 持仓基金可用金额之和
     *
     * @author yunpeng.zhang
     */
    private BigDecimal getUserPortfolioTotalAvailableAmount(List<PortfolioAccountDetailBO> portfolioAccountDetailBOList) {
        BigDecimal userPortfolioTotalAvailableAmount = BigDecimal.ZERO;
        for (PortfolioAccountDetailBO portfolioAccountDetailBO : portfolioAccountDetailBOList) {
            // 持仓基金可用金额
            BigDecimal portfolioFundAvailableAmount = portfolioAccountDetailBO.getAvailableUnit().multiply(portfolioAccountDetailBO.getNav());
            userPortfolioTotalAvailableAmount = userPortfolioTotalAvailableAmount.add(portfolioFundAvailableAmount);
        }
        return userPortfolioTotalAvailableAmount;
    }

    /**
     * 检查用户是否满足调仓前置条件
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    private boolean prepositionCheckUserRebalance(OrderRebalanceCreateBO orderRebalanceCreateBO) {
        if (orderRebalanceCreateBO == null) {
            log.info("{}调仓创建任务：调仓OrderRebalanceCreateBO为空", Thread.currentThread());
            return false;
        }

        long fainModelId = orderRebalanceCreateBO.getFundMainModelId();
        if (fainModelId <= 0L) {
            log.info("{}调仓任务：调仓FundMainModelId异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        String userId = orderRebalanceCreateBO.getUserId();
        if (StringUtils.isEmpty(userId)) {
            log.info("{}调仓任务：调仓userId异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        String thirdPortfolioId = orderRebalanceCreateBO.getThirdPortfolioId();
        if (StringUtils.isEmpty(thirdPortfolioId)) {
            log.info("{}调仓任务：调仓thirdPortfolioId异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        String thirdPortfolioCode = orderRebalanceCreateBO.getThirdPortfolioCode();
        if (StringUtils.isEmpty(thirdPortfolioCode)) {
            log.info("{}调仓任务：调仓thirdPortfolioCode异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        Integer channel = orderRebalanceCreateBO.getChannel();
        if (channel <= 0) {
            log.info("{}调仓任务：调仓channel异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        Long planId = orderRebalanceCreateBO.getPlanId();
        if (planId <= 0L) {
            log.info("{}调仓任务：调仓planId异常，退出调仓！", Thread.currentThread(),
                    JSONObject.toJSONString(orderRebalanceCreateBO));
            return false;
        }

        // 检查用户是否存在已发送到奕丰未完成的订单
        Long count = this.getUnfinishOrderRebalance(orderRebalanceCreateBO);
        if (count > 0L) {
            log.info("{}调仓任务：用户{}存在未处理完的调仓任务数量={}", Thread.currentThread(),
                    orderRebalanceCreateBO.getUserId(), count);
            return false;
        }

        // 用户如果当天存在未授权的调仓订单
        Long currentDayUnAuthCount = this.currentDayUnAuthCount(orderRebalanceCreateBO);
        if (currentDayUnAuthCount > 0) {
            log.info("{}调仓任务：用户{}当天存在未授权的调仓订单={}", Thread.currentThread(),
                    orderRebalanceCreateBO.getUserId(), currentDayUnAuthCount);
            return false;
        }

        return true;
    }

    /**
     * 用户如果当天存在未授权的调仓订单
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-05-05
     */
    private Long currentDayUnAuthCount(OrderRebalanceCreateBO orderRebalanceCreateBO) {
        OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
        orderRebalanceAuthConditionBO.setUserId(orderRebalanceCreateBO.getUserId());
        orderRebalanceAuthConditionBO.setChannel(orderRebalanceCreateBO.getChannel());
        orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        // 用户如果当天存在未授权的调仓订单
        return orderRebalanceAuthMapper.currentDayUnAuthCount(orderRebalanceAuthConditionBO);

    }

    /**
     * 查询用户是调仓订单已发送但未完成的订单
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private Long getUnfinishOrderRebalance(OrderRebalanceCreateBO orderRebalanceCreateBO) {
        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
        orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
        orderRebalanceConditionBO.setUserId(orderRebalanceCreateBO.getUserId());
        orderRebalanceConditionBO.setChannel(orderRebalanceCreateBO.getChannel());
        Long count = orderInfoMapper.queryUnSendOrderRebalance(orderRebalanceConditionBO);
        return count;
    }

    /**
     * 查询主理人模型下的基金占比信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private List<FundMainModelDetailDTO> getFundMainModelDetail(FundMainModelDTO fundMainModelDTO) {
        // 查询主理人模型的基金占比信息
        FundMainModelDetailRequest fundMainModelDetailRequest = new FundMainModelDetailRequest();
        fundMainModelDetailRequest.setFundMainModelId(fundMainModelDTO.getFundMainModelId());
        APIResponse<List<FundMainModelDetailDTO>> fundMainModelDetailListResponse =
                fundMainModelRemote.queryFundMainModelDetailByCondition(fundMainModelDetailRequest);
        List<FundMainModelDetailDTO> fundMainModelDetailDTOList = fundMainModelDetailListResponse.getData();

        if (StringUtils.isEmpty(fundMainModelDetailDTOList)) {
            log.info("{}调仓任务：查询主理人模型的基金占比信息,FundMainModelId={}", Thread.currentThread(),
                    fundMainModelDTO.getFundMainModelId());
            throw new RuntimeException("调仓任务：查询主理人模型的基金占比信息=" + fundMainModelDTO.getFundMainModelId());
        }
        return fundMainModelDetailDTOList;
    }

    /**
     * 调仓任务-查询有效的主理人模型
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private FundMainModelDTO getFundMainModelDTO() {
        // 查询有效的主理人模型
        FundMainModelRequest fundMainModelRequest = new FundMainModelRequest();
        fundMainModelRequest.setFundMainModelStatusEnums(FundMainModelStatusEnums.EFFECTIVE);
        APIResponse<FundMainModelDTO> fundMainModelResponse =
                fundMainModelRemote.queryEffectiveFundMainModel(fundMainModelRequest);
        FundMainModelDTO fundMainModelDTO = fundMainModelResponse.getData();
        if (fundMainModelDTO == null) {
            log.info("{}调仓任务：查询主理人模型信息异常,查询信息={}", Thread.currentThread(),
                    JSONObject.toJSONString(fundMainModelRequest));
            throw new RuntimeException("调仓任务：查询主理人模型信息异常");
        }
        return fundMainModelDTO;
    }

    /**
     * 赋值调仓的组合标准信息到redis中
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-13
     */
    private void setRebalanceStandar2Redis(FundMainModelDTO fundMainModelDTO, List<FundMainModelDetailDTO> fundMainModelDetailDTOList) {
        String thirdPortfolioCode = fundMainModelDTO.getThirdPortfolioCode();
        for (FundMainModelDetailDTO fundMainModelDetail : fundMainModelDetailDTOList) {
            stringRedisTemplate.opsForValue().set(thirdPortfolioCode + ":" + fundMainModelDetail.getFundCode(),
                    fundMainModelDetail.getHoldAmountScale().toString(), 3600 * 12, TimeUnit.SECONDS);
        }
    }

    @Override
    public BaseResponse<YfRebalancePortfolioResponse> confirmRebalance(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception {
        // 更新发送时间
        rebalanceService.updateOrderSendTime(orderRebalanceSendBO);
        // 发送调仓请求到奕丰
        BaseResponse<YfRebalancePortfolioResponse> baseResponse = this.sendRebalancePositions2yifeng(orderRebalanceSendBO);
        log.info("{}调仓交易 ，奕丰响应：{}", Thread.currentThread(), JSONObject.toJSONString(baseResponse));
        return baseResponse;
    }

    @Override
    public boolean repairRebalanceOrder() {
        Integer pagetIndex = 0;
        Integer pageSize = environment.getProperty("rebalance.orderSyncPageSize", Integer.class, 200);
        //1.分页查询调仓历史数据集合
        List<OrderRebalanceAsyncBO> orderRebalanceAsyncBOList = orderRebalanceService.queryOrderRebalanceRepaired(
                FundChannelEnum.YIFENG.getChannel(), OrderBusinessEnum.REBALANCE.getCode(), pagetIndex, pageSize);
        Boolean isAllSuccess = true;
        while (!CollectionUtils.isEmpty(orderRebalanceAsyncBOList)) {
            for (OrderRebalanceAsyncBO orderRebalanceAsyncBO : orderRebalanceAsyncBOList) {
                try {
                    log.info("调仓订单同步开始,orderRebalanceAsyncBO={}", JSONObject.toJSONString(orderRebalanceAsyncBO));
                    // 调仓
                    this.repairOrderRebalanceAsync(orderRebalanceAsyncBO);
                } catch (Exception e) {
                    isAllSuccess = false;
                    log.error("调仓订单同步异常,orderRebalanceAsyncBO={}", JSONObject.toJSONString(orderRebalanceAsyncBO));
                }
            }
            pagetIndex += pageSize;
            orderRebalanceAsyncBOList = orderRebalanceService.queryOrderRebalanceRepaired(
                    FundChannelEnum.YIFENG.getChannel(), OrderBusinessEnum.REBALANCE.getCode(), pagetIndex, pageSize);
        }
        return isAllSuccess;
    }

    public void repairOrderRebalanceAsync(OrderRebalanceAsyncBO orderRebalanceAsyncBO) throws Exception {
        if (orderRebalanceAsyncBO == null) {
            log.info("调仓信息异常，无需同步！orderRebalanceAsyncBO=null");
            return;
        }
        //发送查询订单请求
        SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRebalanceAsyncBO.getAccountNumber(),
                orderRebalanceAsyncBO.getMerchantNumber());
        log.info("修复用户{}的调仓订单同步任务请求：{}",orderRebalanceAsyncBO.getUserId(), JSONObject.toJSONString(syncOrderRequest), Thread.currentThread());

        BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                fundPortfolioService.syncOrder(syncOrderRequest);

        log.info("修复用户{}调仓订单同步任务应答：{}" ,orderRebalanceAsyncBO.getUserId(), JSONObject.toJSONString(baseResponse), Thread.currentThread());
        if (baseResponse.success()) {
            // 订单同步处理
            this.syncOrderRebalanceBiz(orderRebalanceAsyncBO, baseResponse);
        } else {
            log.error("修复用户{}调仓订单同步任务处理异常：{}" ,orderRebalanceAsyncBO.getUserId(), JSONObject.toJSONString(syncOrderRequest), Thread.currentThread());
        }
    }

}
