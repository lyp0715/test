package com.snb.deal.service.impl.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.datetime.DateUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.enums.SmsConfTypeEnum;
import com.snb.common.mq.bean.SmsMessage;
import com.snb.common.mq.enums.Exchange;
import com.snb.deal.admin.api.dto.order.OrderRedemptionDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderRedemptionListRequest;
import com.snb.deal.api.dto.redeem.OrderRedeemAmountResponse;
import com.snb.deal.api.dto.redeem.OrderRedeemFeeResponse;
import com.snb.deal.api.dto.redeem.OrderRedeemResponse;
import com.snb.deal.bo.order.*;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.order.OrderRedeemDO;
import com.snb.deal.entity.order.OrderRedeemDetailDO;
import com.snb.deal.enums.*;
import com.snb.deal.mapper.order.OrderInfoMapper;
import com.snb.deal.mapper.order.OrderRedeemDetailMapper;
import com.snb.deal.mapper.order.OrderRedeemMapper;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderInfoService;
import com.snb.deal.service.order.OrderRedeemService;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailRequest;
import com.snb.fund.api.remote.FundMainModelRemote;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.api.plan.FundPlanService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import com.snb.third.yifeng.dto.order.SyncOrderResponse;
import com.snb.third.yifeng.dto.order.redeem.*;
import com.snb.third.yifeng.dto.plan.YfPortfolioAccount;
import com.snb.third.yifeng.dto.plan.YfPortfolioAccountRequest;
import com.snb.third.yifeng.dto.plan.YfPortfolioAccountResponse;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.dto.user.UserInfoResponse;
import com.snb.user.exception.BusinessException;
import com.snb.user.remote.FundUserRemote;
import com.snb.user.remote.UserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service("orderRedeemService")
@Slf4j
public class OrderRedeemServiceImpl implements OrderRedeemService {

    @Resource
    private OrderRedeemMapper orderRedeemMapper;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Resource
    private OrderRedeemDetailMapper orderRedeemDetailMapper;

    @Autowired
    private GuidCreater guidCreater;

    @Resource
    private FlowNumberService flowNumberService;

    @Resource
    private FundPlanService fundPlanService;

    @Resource
    private FundPortfolioService fundPortfolioService;

    @Resource
    private OrderInfoService orderInfoService;

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;

    @Resource
    private Environment environment;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Reference(version = "1.0")
    private UserRemote userRemote;


    @Reference(version = "1.0")
    private FundMainModelRemote fundMainModelRemote;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public OrderRedeemAmountResponse getOrderRedeemAmount(OrderRedeemBO orderRedeemBO) {
        try {
            YfPortfolioAccountRequest yfPortfolioAccountRequest = new YfPortfolioAccountRequest();
            yfPortfolioAccountRequest.setPortfolioId(Integer.parseInt(orderRedeemBO.getPortfolioId()));
            yfPortfolioAccountRequest.setAccountNumber(orderRedeemBO.getAccountNumber());
            log.info("getOrderRedeemAmount-发送请求:{}", yfPortfolioAccountRequest.toString());
            BaseResponse<YfPortfolioAccountResponse> result = (BaseResponse<YfPortfolioAccountResponse>) fundPlanService.queryPortfolioAccount(yfPortfolioAccountRequest);
            log.info("getOrderRedeemAmount-获取结果:{}", result.toString());
            if (!result.success() || result.getData() == null || CollectionUtils.isEmpty(result.getData().getData())) {
                return null;
            }
            YfPortfolioAccount portfolioAccount = result.getData().getData().get(0);
            OrderRedeemAmountResponse orderRedeemAmountResponse = new OrderRedeemAmountResponse();
            orderRedeemAmountResponse.setAvailableAmount(this.formatDecimal(portfolioAccount.getAvailableAmount()));
            orderRedeemAmountResponse.setMaxRedemptionAmount(this.formatDecimal(portfolioAccount.getMaxRedemptionAmount()));
            orderRedeemAmountResponse.setMinRedemptionAmount(this.formatDecimal(portfolioAccount.getMinRedemptionAmount()));
            orderRedeemAmountResponse.setMinRetainAmount(this.formatDecimal(portfolioAccount.getMinRetainAmount()));
            return orderRedeemAmountResponse;
        } catch (Exception e) {
            log.error("getOrderRedeemAmount is error", e);
        }
        return null;
    }

    @Override
    public ResultCode checkOrderRedeem(OrderRedeemBO orderRedeemBO) {
        OrderRedeemAmountResponse orderRedeemAmountResponse = this.getOrderRedeemAmount(orderRedeemBO);
        if (orderRedeemAmountResponse == null) {
            return ResultCode.PORTFOLIO_ACCOUNT_ERROR;
        }
        BigDecimal availableAmount = orderRedeemAmountResponse.getAvailableAmount();
        BigDecimal maxRedemptionAmount = orderRedeemAmountResponse.getMaxRedemptionAmount();
        BigDecimal minRedemptionAmount = orderRedeemAmountResponse.getMinRedemptionAmount();
        if (availableAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ResultCode.PORTFOLIO_AVAILABLE_AMOUNT;
        }
        if (maxRedemptionAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ResultCode.PORTFOLIO_MAX_REDEMPTION_AMOUNT;
        }
        if (minRedemptionAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ResultCode.PORTFOLIO_MIN_REDEMPTION_AMOUNT;
        }
        if (orderRedeemBO.getTransactionAmount().compareTo(minRedemptionAmount) < 0) {
            return ResultCode.MIN_REDEMPTION_AMOUNT_ERROR;
        }
        if (orderRedeemBO.getTransactionAmount().compareTo(availableAmount) > 0) {
            return ResultCode.AVAILABLE_AMOUNT_ERROR;
        }
        if (orderRedeemBO.getTransactionAmount().compareTo(maxRedemptionAmount) > 0
                && orderRedeemBO.getTransactionAmount().compareTo(availableAmount) < 0) {
            return ResultCode.MAX_REDEMPTION_AVAILABLE_AMOUNT_ERROR;
        }
        return ResultCode.REDEEM_CHECK_SUCCESS;
    }

    /**
     * @param orderRedeemBO
     * @return OrderRedeemResponse
     * @Description
     * @author lizengqiang
     * @date 2018/4/10 14:44
     */
    @Transactional
    @Override
    public OrderRedeemDO createOrderRedeem(OrderRedeemBO orderRedeemBO) throws Exception {
        log.info("创建赎回订单开始-orderRedeemRequest:{}", orderRedeemBO.toString());

        OrderRedeemDO orderRedeemDO = OrderRedeemDO.build(OrderRedeemDO.OrderRedeemBuildEnum.INIT);
        //此处查询赎回预计手续费
        try {
            OrderRedeemFeeResponse orderRedeemFeeResponse = this.orderRedeemFee(orderRedeemBO);
            if (orderRedeemFeeResponse != null) {
                orderRedeemDO.setTransactionCharge(this.formatDecimal(orderRedeemFeeResponse.getTotalRedemptionCharge()));
            }
        } catch (Exception e) {
            log.error("创建赎回订单，查询预计手续费异常{}",orderRedeemBO,e);
        }

        //插入订单主表
        OrderInfoDO orderInfoDO = OrderInfoDO.build(OrderInfoDO.OrderInfoBuildEnum.INIT, OrderBusinessEnum.REDEEM);
        orderInfoDO.setOrderNo(guidCreater.getUniqueID());
        orderInfoDO.setUserId(orderRedeemBO.getUserId());
        orderInfoDO.setTransactionAmount(orderRedeemBO.getTransactionAmount());
        orderInfoMapper.insert(orderInfoDO);

        //插入订单赎回表
        orderRedeemDO.setOrderRedeemId(guidCreater.getUniqueID());
        orderRedeemDO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
        orderRedeemDO.setOrderNo(orderInfoDO.getOrderNo());
        orderRedeemDO.setTransactionAmount(orderInfoDO.getTransactionAmount());
        orderRedeemDO.setUserId(orderInfoDO.getUserId());
        orderRedeemDO.setAccountNumber(orderRedeemBO.getAccountNumber());
        orderRedeemDO.setPortfolioId(orderRedeemBO.getPortfolioId());
        orderRedeemDO.setPortfolioCode(orderRedeemBO.getPortfolioCode());
        orderRedeemDO.setInvestorPayId(orderRedeemBO.getInvestorPayId());
        orderRedeemMapper.insert(orderRedeemDO);
        log.info("创建赎回订单结束-orderInfoDO:{},orderRedeemDO:{}", orderInfoDO.toString(), orderRedeemDO.toString());
        return orderRedeemDO;
    }

    @Override
    public List<OrderRedeemDO> queryReceiving(int limit) {
        return orderRedeemMapper.queryReceiving(limit);
    }

    @Transactional
    @Override
    public OrderRedeemResponse compensateOrderRedeem(BaseResponse<RedeemPortfolioResponse> baseResponse, OrderRedeemDO orderRedeemDO) {
        OrderRedeemResponse result = new OrderRedeemResponse();
        result.setOrderNo(orderRedeemDO.getOrderNo());
        result.setOrderStatus(OrderStatusEnum.PROCESS.getCode());
        log.info("compensateOrderRedeem-baseResponse:{},orderRedeemDO:{}", baseResponse.toString(), orderRedeemDO.toString());
        OrderRedeemDO orderRedeemDOParam = new OrderRedeemDO(orderRedeemDO.getOrderNo());
        if (baseResponse.success()) {
            //更新订单赎回表
            orderRedeemDOParam.setTransactionStatus(TransactionStatusEnum.RECEIVED.getCode());
            orderRedeemDOParam.setResponseCode(baseResponse.getCode());
            orderRedeemDOParam.setResponseMessage(baseResponse.getMessage());
            orderRedeemMapper.updateByOrderNo(orderRedeemDOParam);
        } else {
            //更新订单表
            orderInfoMapper.update(new OrderInfoDO(orderRedeemDO.getOrderNo(), OrderStatusEnum.APPLY_FAIL.getCode()));
            //更新订单赎回表
            orderRedeemDOParam.setTransactionStatus(TransactionStatusEnum.FAIL.getCode());
            orderRedeemDOParam.setResponseCode(baseResponse.getCode());
            orderRedeemDOParam.setResponseMessage(baseResponse.getMessage());
            orderRedeemMapper.updateByOrderNo(orderRedeemDOParam);
            result.setOrderStatus(OrderStatusEnum.APPLY_FAIL.getCode());
        }
        if (baseResponse.success() && baseResponse.getData() != null) {
            RedeemPortfolioResponse redeemPortfolioResponse = baseResponse.getData();
            log.info("补偿赎回信息开始-redeemPortfolioResponse-{}", redeemPortfolioResponse.toString());
            //插入订单赎回详情表
            List<OrderRedeemDetailDO> orderRedeemDetailDOList = this.buildOrderRedeemDetailList(redeemPortfolioResponse, orderRedeemDO);
            if (CollectionUtils.isNotEmpty(orderRedeemDetailDOList)) {
                orderRedeemDetailMapper.insertBatch(orderRedeemDetailDOList);
                log.info("补偿赎回信息结束-orderRedeemDetailDOList-{}", orderRedeemDetailDOList.toString());
            }
        }
        return result;
    }

    /**
     * @param orderRedeemResponse
     * @param orderRedeemDO
     * @return
     * @Description 赎回完成页设置赎回金额、赎回费用、赎回基金交易预计确认日期、到帐日期
     * @author lizengqiang
     * @date 2018/6/6 11:16
     */
    public void orderRedeemComplete(OrderRedeemResponse orderRedeemResponse, RedeemPortfolioResponse redeemPortfolioResponse, OrderRedeemDO orderRedeemDO) {
        try {
//            OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
//            orderRedeemBO.setPortfolioId(orderRedeemDO.getPortfolioId());
//            orderRedeemBO.setAccountNumber(orderRedeemDO.getAccountNumber());
//            orderRedeemBO.setTransactionAmount(orderRedeemDO.getTransactionAmount());
            //1.计算赎回金额
            orderRedeemResponse.setTransactionAmount(this.formatDecimal(orderRedeemDO.getTransactionAmount()));
            orderRedeemResponse.setTransactionCharge(orderRedeemDO.getTransactionCharge());
            //2.计算赎回费用
            /*OrderRedeemFeeResponse orderRedeemFeeResponse = this.orderRedeemFee(orderRedeemBO);
            if (orderRedeemFeeResponse != null) {
                orderRedeemResponse.setTransactionCharge(this.formatDecimal(orderRedeemFeeResponse.getTotalRedemptionCharge()));
            }*/
        } catch (Exception e) {
            log.error("orderRedeemComplete is error:orderRedeemDO:{}", orderRedeemDO.toString(), e);
        }
        if (redeemPortfolioResponse == null || CollectionUtils.isEmpty(redeemPortfolioResponse.getSellResult())) {
            return;
        }
        try {
            List<OrderRedeemExpectedDateBO> expectedConfirmedDateList = this.buildExpectedConfirmedDate(redeemPortfolioResponse);
            if (CollectionUtils.isNotEmpty(expectedConfirmedDateList)) {
                expectedConfirmedDateList.sort((OrderRedeemExpectedDateBO o1, OrderRedeemExpectedDateBO o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
                orderRedeemResponse.setExpectedConfirmedDate(DateTimeUtil.format(expectedConfirmedDateList.get(0).getExpectedConfirmedDate(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
                orderRedeemResponse.setExpectedConfirmedWeak(DateTimeUtil.dateToWeek(expectedConfirmedDateList.get(0).getExpectedConfirmedDate()));
            }
            List<OrderRedeemExpectedDateBO> expectedDealDateList = this.buildExpectedDealDate(redeemPortfolioResponse);
            if (CollectionUtils.isNotEmpty(expectedDealDateList)) {
                expectedDealDateList.sort((OrderRedeemExpectedDateBO o1, OrderRedeemExpectedDateBO o2) -> o2.getExpectedDealDate().compareTo(o1.getExpectedDealDate()));
                orderRedeemResponse.setExpectedDealDate(DateTimeUtil.format(expectedDealDateList.get(0).getExpectedDealDate(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
                orderRedeemResponse.setExpectedDealWeak(DateTimeUtil.dateToWeek(expectedDealDateList.get(0).getExpectedDealDate()));
            }
        } catch (Exception e) {
            log.error("orderRedeemComplete is error,redeemPortfolioResponse-{}", redeemPortfolioResponse.toString(), e);
        }
    }


    private List<OrderRedeemExpectedDateBO> buildExpectedConfirmedDate(RedeemPortfolioResponse redeemPortfolioResponse) {
        List<OrderRedeemExpectedDateBO> result = Lists.newArrayList();
        for (RedeemPortfolioDetail redeemPortfolioDetail : redeemPortfolioResponse.getSellResult()) {
            Date expectedCofirmedDate = DateTimeUtil.parseDate(redeemPortfolioDetail.getExpectedCofirmedDate());
            if (expectedCofirmedDate != null) {
                OrderRedeemExpectedDateBO orderRedeemExpectedDateBO = new OrderRedeemExpectedDateBO();
                orderRedeemExpectedDateBO.setExpectedConfirmedDate(expectedCofirmedDate);
                result.add(orderRedeemExpectedDateBO);
            }
        }
        return result;
    }

    private List<OrderRedeemExpectedDateBO> buildExpectedDealDate(RedeemPortfolioResponse redeemPortfolioResponse) {
        List<OrderRedeemExpectedDateBO> result = Lists.newArrayList();
        for (RedeemPortfolioDetail redeemPortfolioDetail : redeemPortfolioResponse.getSellResult()) {
            Date expectedDealDate = DateTimeUtil.parseDate(redeemPortfolioDetail.getExpectedDealDate());
            if (expectedDealDate != null) {
                OrderRedeemExpectedDateBO orderRedeemExpectedDateBO = new OrderRedeemExpectedDateBO();
                orderRedeemExpectedDateBO.setExpectedDealDate(expectedDealDate);
                result.add(orderRedeemExpectedDateBO);
            }
        }
        return result;
    }

    /**
     * @param redeemPortfolioResponse
     * @param orderRedeemDO
     * @return
     * @Description
     * @author lizengqiang
     * @date 2018/4/28 15:12
     */
    private List<OrderRedeemDetailDO> buildOrderRedeemDetailList(RedeemPortfolioResponse redeemPortfolioResponse, OrderRedeemDO orderRedeemDO) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        if (redeemPortfolioResponse == null) {
            return result;
        }
        List<RedeemPortfolioDetail> redeemPortfolioDetailList = redeemPortfolioResponse.getSellResult();
        if (CollectionUtils.isEmpty(redeemPortfolioDetailList)) {
            return result;
        }
        for (RedeemPortfolioDetail redeemPortfolioDetail : redeemPortfolioDetailList) {
            OrderRedeemDetailDO orderRedeemDetailDO = new OrderRedeemDetailDO();
            orderRedeemDetailDO.setOrderRedeemDetailId(guidCreater.getUniqueID());
            orderRedeemDetailDO.setOrderRedeemId(orderRedeemDO.getOrderRedeemId());
            orderRedeemDetailDO.setUserId(StringUtils.defaultString(orderRedeemDO.getUserId()));
            orderRedeemDetailDO.setAccountNumber(StringUtils.defaultString(orderRedeemDO.getAccountNumber()));
            orderRedeemDetailDO.setBankCode(StringUtils.EMPTY);
            orderRedeemDetailDO.setBankNumber(StringUtils.EMPTY);
            orderRedeemDetailDO.setCancelEnable(StringUtils.EMPTY);
            orderRedeemDetailDO.setCanceledDate(null);
            orderRedeemDetailDO.setCompletedDate(null);
            orderRedeemDetailDO.setContractNumber(StringUtils.defaultString(redeemPortfolioDetail.getContractNumber()));
            orderRedeemDetailDO.setDiscountRate(BigDecimal.ZERO);
            orderRedeemDetailDO.setDiscountTransactionCharge(BigDecimal.ZERO);
            orderRedeemDetailDO.setExpectedConfirmedDate(DateTimeUtil.parseDate(redeemPortfolioDetail.getExpectedCofirmedDate()));
            orderRedeemDetailDO.setFundCode(StringUtils.defaultString(redeemPortfolioDetail.getFundCode()));
            orderRedeemDetailDO.setFundName(StringUtils.defaultString(redeemPortfolioDetail.getFundName()));
            orderRedeemDetailDO.setInvestorPayId(null);
            orderRedeemDetailDO.setMerchantNumber(StringUtils.defaultString(orderRedeemDO.getMerchantNumber()));
            orderRedeemDetailDO.setOrderDate(DateTimeUtil.parseDate(redeemPortfolioDetail.getOrderDate()));
            orderRedeemDetailDO.setPayMethod(1);
            orderRedeemDetailDO.setPortfolioId(StringUtils.defaultString(orderRedeemDO.getPortfolioId()));
            orderRedeemDetailDO.setPricedDate(null);
            orderRedeemDetailDO.setReason(StringUtils.defaultString(redeemPortfolioDetail.getReason()));
            orderRedeemDetailDO.setRspId(null);
            orderRedeemDetailDO.setSettlementDate(null);
            orderRedeemDetailDO.setTransactionAmount(BigDecimal.ZERO);
            orderRedeemDetailDO.setTransactionCharge(BigDecimal.ZERO);
            orderRedeemDetailDO.setTransactionDate(DateTimeUtil.parseDate(redeemPortfolioDetail.getTransactionDate()));
            orderRedeemDetailDO.setTransactionPrice(BigDecimal.ZERO);
            orderRedeemDetailDO.setTransactionRate(BigDecimal.ZERO);
            TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, redeemPortfolioDetail.getTransactionStatus());
            orderRedeemDetailDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
            orderRedeemDetailDO.setTransactionType(OrderBusinessEnum.REDEEM.getCode());
            orderRedeemDetailDO.setTransactionUnit(this.formatDecimal(redeemPortfolioDetail.getTransactionUnit()));
            orderRedeemDetailDO.setVoidDate(null);
            orderRedeemDetailDO.setExpectedDealDate(DateTimeUtil.parseDate(redeemPortfolioDetail.getExpectedDealDate()));
            orderRedeemDetailDO.setPortfolioCode(StringUtils.defaultString(orderRedeemDO.getPortfolioCode()));
            result.add(orderRedeemDetailDO);
        }
        return result;
    }

    @Override
    public List<OrderRedeemAsyncBO> queryOrderRedeemAsync(int pageNo, int pageSize) {
        List<OrderRedeemAsyncBO> result = Lists.newArrayList();
        //1.查询处理中的订单
        List<OrderRedeemDO> orderRedeemDOList = orderRedeemMapper.queryOrder(OrderStatusEnum.PROCESS.getCode(),
                FundChannelEnum.YIFENG.getChannel(), OrderBusinessEnum.REDEEM.getCode(), pageNo*pageSize,pageSize);
        if (CollectionUtils.isEmpty(orderRedeemDOList)) {
            return result;
        }
        for (OrderRedeemDO orderRedeemDO : orderRedeemDOList) {
            OrderRedeemAsyncBO orderRedeemAsyncBO = new OrderRedeemAsyncBO();
            BeanUtils.copyProperties(orderRedeemDO, orderRedeemAsyncBO);
            result.add(orderRedeemAsyncBO);
        }
        return result;
    }

    //    @Event(reliability = false,
//            queue = "",
//            exchange = "exchange.orderRedeem.syncNotify"
//    )
    @Transactional
    @Override
    public void syncOrderRedeem(OrderRedeemAsyncBO orderRedeemAsyncBO, BaseResponse<SyncOrderListResponse> baseResponse) throws Exception {
        if (!baseResponse.success() || baseResponse.getData() == null) {
            return;
        }
        log.info("{}:syncOrderRedeem-开始:orderRedeemAsyncBO:{}，baseResponse:{}", Thread.currentThread(), orderRedeemAsyncBO.toString(), baseResponse.toString());
        List<SyncOrderResponse> syncOrderResponseList = baseResponse.getData().getData();
        if (CollectionUtils.isEmpty(syncOrderResponseList)) {
            return;
        }
        List<OrderRedeemDetailDO> orderRedeemDetailDOList = orderRedeemDetailMapper.queryByRedeemId(orderRedeemAsyncBO.getOrderRedeemId());
        for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
            List<OrderRedeemDetailDO> orderRedeemDetailDOs = this.orderRedeemDetailContain(orderRedeemDetailDOList, syncOrderResponse);
            if (CollectionUtils.isNotEmpty(orderRedeemDetailDOs)) {
                for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailDOs) {
                    //修改
                    this.updateOrderRedeemDetail(orderRedeemDetailDO, syncOrderResponse);
                }
            } else {
                //插入
                this.saveOrderRedeemDetail(orderRedeemAsyncBO, syncOrderResponse);
            }
        }
        if (CollectionUtils.isNotEmpty(orderRedeemDetailDOList)) {
            for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailDOList) {
                if (!this.orderRedeemDetailContain(syncOrderResponseList, orderRedeemDetailDO)) {
                    //删除
                    orderRedeemDetailMapper.delete(orderRedeemDetailDO.getOrderRedeemDetailId());
                }
            }
        }
//        //当同步组合下的基金都为交易成功时更新订单主表和订单赎回表
//        this.orderRedeemSuccess(syncOrderResponseList, orderRedeemAsyncBO);
//        //当同步组合下的基金都为交易失败时更新订单主表和订单赎回表
//        this.orderRedeemFail(syncOrderResponseList, orderRedeemAsyncBO);
//        //部分赎回成功-银行卡
//        this.orderRedeemCompleteMsg(syncOrderResponseList, orderRedeemAsyncBO);
        this.orderRedeemComplete(syncOrderResponseList, orderRedeemAsyncBO);
        //发送通知
//        try {
//            OrderRedeemSyncNotify orderRedeemSyncNotify = new OrderRedeemSyncNotify();
//            BeanUtils.copyProperties(orderRedeemAsyncBO, orderRedeemSyncNotify);
//            EventMessageContext.addMessage(orderRedeemSyncNotify);
//        } catch (Exception e) {
//            log.error("syncOrderRedeem-exchange.orderRedeem.syncNotify is error,orderRedeemAsyncBO:{}", orderRedeemAsyncBO.toString(), e);
//        }
        log.info("{}:syncOrderRedeem-结束:orderRedeemAsyncBO:{}，baseResponse:{}", Thread.currentThread(), orderRedeemAsyncBO.toString(), baseResponse.toString());
    }

    private List<OrderFeeRateBO> buildOrderFeeList(List<YfRedeemPortfolioFeeDetail> feeDetails) {
        List<OrderFeeRateBO> orderFeeList = Lists.newArrayList();
        for (YfRedeemPortfolioFeeDetail redeemPortfolioFeeDetail : feeDetails) {
            OrderFeeRateBO orderFeeRateBO = new OrderFeeRateBO();
            orderFeeRateBO.setFeeRate(redeemPortfolioFeeDetail.getAvgRedemptionChargeRate());
            orderFeeRateBO.setFundCode(redeemPortfolioFeeDetail.getFundCode());
            orderFeeList.add(orderFeeRateBO);
        }
        return orderFeeList;

    }

    /**
     * @param orderRedeemDetailDOList
     * @param syncOrderResponse
     * @return
     * @Description 遍历该组合的赎回详情，如果syncOrderResponse中的基金代码在已有的赎回详情表中，
     * 则将对该基金代码的赎回详情信息进行更新，如果没有则进行插入
     * @author lizengqiang
     * @date 2018/5/4 20:54
     */
    private List<OrderRedeemDetailDO> orderRedeemDetailContain(List<OrderRedeemDetailDO> orderRedeemDetailDOList, SyncOrderResponse syncOrderResponse) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(orderRedeemDetailDOList)) {
            for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailDOList) {
                if (StringUtils.equals(orderRedeemDetailDO.getFundCode(), syncOrderResponse.getFundCode())) {
                    result.add(orderRedeemDetailDO);
                }
            }
        }
        return result;
    }

    /**
     * @param syncOrderResponseList
     * @param orderRedeemDetailDO
     * @return
     * @Description 遍历该组合详情表中的数据，如果其中一条不在最新同步的订单详情列表中则进行删除，通过基金代码进行判断
     * @author lizengqiang
     * @date 2018/5/4 21:45
     */
    private Boolean orderRedeemDetailContain(List<SyncOrderResponse> syncOrderResponseList, OrderRedeemDetailDO orderRedeemDetailDO) {
        if (CollectionUtils.isEmpty(syncOrderResponseList)) {
            return false;
        }
        for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
            if (StringUtils.equals(orderRedeemDetailDO.getFundCode(), syncOrderResponse.getFundCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param orderRedeemAsyncBO
     * @param syncOrderResponse
     * @return
     * @Description 同步订单详情插入OrderRedeemDetail
     * @author lizengqiang
     * @date 2018/4/13 19:08
     */
    private void saveOrderRedeemDetail(OrderRedeemAsyncBO orderRedeemAsyncBO, SyncOrderResponse syncOrderResponse) {
        //插入
        OrderRedeemDetailDO orderRedeemDetailDO = new OrderRedeemDetailDO();
        orderRedeemDetailDO.setOrderRedeemDetailId(guidCreater.getUniqueID());
        orderRedeemDetailDO.setOrderRedeemId(orderRedeemAsyncBO.getOrderRedeemId());
        orderRedeemDetailDO.setUserId(StringUtils.defaultString(orderRedeemAsyncBO.getUserId()));
        orderRedeemDetailDO.setAccountNumber(StringUtils.defaultString(orderRedeemAsyncBO.getAccountNumber()));
        orderRedeemDetailDO.setBankCode(StringUtils.defaultString(syncOrderResponse.getBankCode()));
        orderRedeemDetailDO.setBankNumber(StringUtils.defaultString(syncOrderResponse.getBankNumber()));
        orderRedeemDetailDO.setCancelEnable(StringUtils.defaultString(syncOrderResponse.getCancelEnable()));
        orderRedeemDetailDO.setCanceledDate(DateTimeUtil.parseDate((syncOrderResponse.getCanceledDate())));
        orderRedeemDetailDO.setCompletedDate(DateTimeUtil.parseDate((syncOrderResponse.getCompletedDate())));
        orderRedeemDetailDO.setContractNumber(StringUtils.defaultString(syncOrderResponse.getContractNumber()));
        orderRedeemDetailDO.setDiscountRate(this.formatDecimal(syncOrderResponse.getDiscountRate()));
        orderRedeemDetailDO.setDiscountTransactionCharge(this.formatDecimal(syncOrderResponse.getDiscountTransactionCharge()));
        orderRedeemDetailDO.setExpectedConfirmedDate(DateTimeUtil.parseDate((syncOrderResponse.getExpectedConfirmedDate())));
        orderRedeemDetailDO.setFundCode(StringUtils.defaultString(syncOrderResponse.getFundCode()));
        orderRedeemDetailDO.setFundName(StringUtils.defaultString(syncOrderResponse.getFundName()));
        orderRedeemDetailDO.setInvestorPayId(StringUtils.isNumeric(syncOrderResponse.getInvestorPayId()) ? Integer.parseInt(syncOrderResponse.getInvestorPayId()) : null);
        orderRedeemDetailDO.setMerchantNumber(StringUtils.defaultString(orderRedeemAsyncBO.getMerchantNumber()));
        orderRedeemDetailDO.setOrderDate(DateTimeUtil.parseDate((syncOrderResponse.getOrderDate())));
        orderRedeemDetailDO.setPayMethod(StringUtils.isNumeric(syncOrderResponse.getPayMethod()) ? Integer.parseInt(syncOrderResponse.getPayMethod()) : null);
        orderRedeemDetailDO.setPortfolioId(StringUtils.defaultString(syncOrderResponse.getPortfolioId()));
        orderRedeemDetailDO.setPricedDate(DateTimeUtil.parseDate((syncOrderResponse.getPricedDate())));
        orderRedeemDetailDO.setReason(StringUtils.defaultString(syncOrderResponse.getReason()));
        orderRedeemDetailDO.setRspId(StringUtils.isNumeric(syncOrderResponse.getRspId()) ? Integer.parseInt(syncOrderResponse.getRspId()) : null);
        orderRedeemDetailDO.setSettlementDate(DateTimeUtil.parseDate((syncOrderResponse.getSettlementDate())));
        orderRedeemDetailDO.setTransactionAmount(this.formatDecimal(syncOrderResponse.getTransactionAmount()));
        orderRedeemDetailDO.setTransactionCharge(this.formatDecimal(syncOrderResponse.getTransactionCharge()));
        orderRedeemDetailDO.setTransactionDate(DateTimeUtil.parseDate((syncOrderResponse.getTransactionDate())));
        orderRedeemDetailDO.setTransactionPrice(this.formatDecimal(syncOrderResponse.getTransactionPrice()));
        orderRedeemDetailDO.setTransactionRate(this.formatDecimal(syncOrderResponse.getTransactionRate()));
        TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
        orderRedeemDetailDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
        orderRedeemDetailDO.setTransactionType(OrderBusinessEnum.REDEEM.getCode());
        orderRedeemDetailDO.setTransactionUnit(this.formatDecimal(syncOrderResponse.getTransactionUnit()));
        orderRedeemDetailDO.setVoidDate(DateTimeUtil.parseDate((syncOrderResponse.getVoidDate())));
        orderRedeemDetailDO.setExpectedDealDate(null);
        orderRedeemDetailDO.setPortfolioCode(StringUtils.defaultString(orderRedeemAsyncBO.getPortfolioCode()));
        orderRedeemDetailMapper.insert(orderRedeemDetailDO);
    }

    /**
     * @param orderRedeemDetailDO
     * @param syncOrderResponse
     * @return
     * @Description 同步订单详情修改OrderRedeemDetail
     * @author lizengqiang
     * @date 2018/4/13 19:08
     */
    private void updateOrderRedeemDetail(OrderRedeemDetailDO orderRedeemDetailDO, SyncOrderResponse syncOrderResponse) {
        //更新
        orderRedeemDetailDO.setBankCode(syncOrderResponse.getBankCode());
        orderRedeemDetailDO.setBankNumber(syncOrderResponse.getBankNumber());
        orderRedeemDetailDO.setCancelEnable(syncOrderResponse.getCancelEnable());
        orderRedeemDetailDO.setCanceledDate(DateTimeUtil.parseDate((syncOrderResponse.getCanceledDate())));
        orderRedeemDetailDO.setCompletedDate(DateTimeUtil.parseDate((syncOrderResponse.getCompletedDate())));
        orderRedeemDetailDO.setContractNumber(syncOrderResponse.getContractNumber());
        orderRedeemDetailDO.setDiscountRate(this.formatNull(syncOrderResponse.getDiscountRate()));
        orderRedeemDetailDO.setDiscountTransactionCharge(this.formatNull(syncOrderResponse.getDiscountTransactionCharge()));
        orderRedeemDetailDO.setExpectedConfirmedDate(DateTimeUtil.parseDate((syncOrderResponse.getExpectedConfirmedDate())));
        orderRedeemDetailDO.setFundCode(syncOrderResponse.getFundCode());
        orderRedeemDetailDO.setFundName(syncOrderResponse.getFundName());
        orderRedeemDetailDO.setInvestorPayId(StringUtils.isNumeric(syncOrderResponse.getInvestorPayId()) ? Integer.parseInt(syncOrderResponse.getInvestorPayId()) : null);
        orderRedeemDetailDO.setOrderDate(DateTimeUtil.parseDate((syncOrderResponse.getOrderDate())));
        orderRedeemDetailDO.setPayMethod(StringUtils.isNumeric(syncOrderResponse.getPayMethod()) ? Integer.parseInt(syncOrderResponse.getPayMethod()) : null);
        orderRedeemDetailDO.setPortfolioId(syncOrderResponse.getPortfolioId());
        orderRedeemDetailDO.setPricedDate(DateTimeUtil.parseDate((syncOrderResponse.getPricedDate())));
        orderRedeemDetailDO.setReason(StringUtils.defaultString(syncOrderResponse.getReason()));
        orderRedeemDetailDO.setRspId(StringUtils.isNumeric(syncOrderResponse.getRspId()) ? Integer.parseInt(syncOrderResponse.getRspId()) : null);
        orderRedeemDetailDO.setSettlementDate(DateTimeUtil.parseDate((syncOrderResponse.getSettlementDate())));
        orderRedeemDetailDO.setTransactionAmount(this.formatNull(syncOrderResponse.getTransactionAmount()));
        orderRedeemDetailDO.setTransactionCharge(this.formatNull(syncOrderResponse.getTransactionCharge()));
        orderRedeemDetailDO.setTransactionDate(DateTimeUtil.parseDate((syncOrderResponse.getTransactionDate())));
        orderRedeemDetailDO.setTransactionPrice(this.formatNull(syncOrderResponse.getTransactionPrice()));
        orderRedeemDetailDO.setTransactionRate(this.formatNull(syncOrderResponse.getTransactionRate()));
        TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
        orderRedeemDetailDO.setTransactionStatus(transactionStatusEnum == null ? null : transactionStatusEnum.getCode());
        orderRedeemDetailDO.setTransactionUnit(this.formatNull(syncOrderResponse.getTransactionUnit()));
        orderRedeemDetailDO.setVoidDate(DateTimeUtil.parseDate((syncOrderResponse.getVoidDate())));
        orderRedeemDetailMapper.update(orderRedeemDetailDO);
    }

    private void orderRedeemComplete(List<SyncOrderResponse> syncOrderResponseList, OrderRedeemAsyncBO orderRedeemAsyncBO) {
        int total = syncOrderResponseList.size();
        int success = 0;
        int fail = 0;
        BigDecimal transactionAmountSuccess = BigDecimal.ZERO;
        BigDecimal transactionChargeSuccess = BigDecimal.ZERO;
        BigDecimal transactionAmountFail = BigDecimal.ZERO;
        BigDecimal transactionChargeFail = BigDecimal.ZERO;
        for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
            TransactionStatusEnum transactionStatusEnum_ = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
            if (transactionStatusEnum_ == TransactionStatusEnum.SUCCESS) {
                if (StringUtils.isNotEmpty(syncOrderResponse.getTransactionAmount())) {
                    transactionAmountSuccess = transactionAmountSuccess.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
                }
                if (StringUtils.isNotEmpty(syncOrderResponse.getDiscountTransactionCharge())) {
                    transactionChargeSuccess = transactionChargeSuccess.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
                }
                success++;
            } else if (transactionStatusEnum_ == TransactionStatusEnum.FAIL || transactionStatusEnum_ == TransactionStatusEnum.PAY_FAIL) {
                if (StringUtils.isNotEmpty(syncOrderResponse.getTransactionAmount())) {
                    transactionAmountFail = transactionAmountFail.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
                }
                if (StringUtils.isNotEmpty(syncOrderResponse.getDiscountTransactionCharge())) {
                    transactionChargeFail = transactionChargeFail.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
                }
                fail++;
            }
        }
        if (success == total) {
            //1.更新订单主表
            orderInfoMapper.update(new OrderInfoDO(orderRedeemAsyncBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
            //2.更新订单赎回表
            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
            orderRedeemDO.setTransactionStatus(TransactionStatusEnum.SUCCESS.getCode());
            orderRedeemDO.setTransactionAmount(transactionAmountSuccess);
            orderRedeemDO.setTransactionCharge(transactionChargeSuccess);
            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
            //全部赎回成功发送短信
            this.orderRedeemSuccessMsg(orderRedeemAsyncBO, transactionAmountSuccess, transactionChargeSuccess);
        } else if (fail == total) {
            //1.更新订单主表
            orderInfoMapper.update(new OrderInfoDO(orderRedeemAsyncBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
            //2.更新订单赎回表
            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
            orderRedeemDO.setTransactionStatus(TransactionStatusEnum.FAIL.getCode());
            orderRedeemDO.setTransactionAmount(transactionAmountFail);
            orderRedeemDO.setTransactionCharge(transactionChargeFail);
            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
            //全部赎回失败发送短信
            this.orderRedeemFailMsg(orderRedeemAsyncBO, transactionAmountFail, orderRedeemAsyncBO.getCreateTime());
        } else if (success > 0 && fail > 0 && (success + fail == total)) {
            //1.更新订单主表
            orderInfoMapper.update(new OrderInfoDO(orderRedeemAsyncBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
            //2.更新订单赎回表
            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
            orderRedeemDO.setTransactionStatus(TransactionStatusEnum.SUCCESS_PART.getCode());
            orderRedeemDO.setTransactionAmount(transactionAmountSuccess);
            orderRedeemDO.setTransactionCharge(transactionChargeSuccess);
            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
            //部分赎回成功-银行卡
            this.orderRedeemCompleteMsg(transactionAmountSuccess, transactionChargeSuccess, transactionAmountFail, orderRedeemAsyncBO);
        } else {
            BigDecimal transactionAmount = BigDecimal.ZERO;
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
                TransactionStatusEnum transactionStatus = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
                if (transactionStatus == TransactionStatusEnum.FAIL || transactionStatus == TransactionStatusEnum.PAY_FAIL) {
                    continue;
                }
                if (StringUtils.isNotEmpty(syncOrderResponse.getTransactionAmount())) {
                    transactionAmount = transactionAmount.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
                }
                if (StringUtils.isNotEmpty(syncOrderResponse.getDiscountTransactionCharge())) {
                    transactionCharge = transactionCharge.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
                }
            }
            //更新订单赎回表
            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
            orderRedeemDO.setTransactionAmount(transactionAmount);
            orderRedeemDO.setTransactionCharge(transactionCharge);
            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
        }
    }


    private Boolean sendMsgOnlyOnce(Long orderNo) {
        if (orderNo == null || orderNo.longValue() == 0L) {
            return false;
        }
        try {
            String key = new StringBuffer("redeemMsg_").append(orderNo).toString();
            Long count = stringRedisTemplate.opsForValue().increment(key, 1);
            if (count > 1) {
                return false;
            }
        } catch (Exception e) {
            log.error("sendMsgOnlyOnce is error,orderNo:{}", orderNo, e);
        }
        return true;
    }
//    /**
//     * @param syncOrderResponseList
//     * @param orderRedeemAsyncBO
//     * @return
//     * @Description 当同步组合下的基金都为交易成功时更新订单主表和订单赎回表
//     * @author lizengqiang
//     * @date 2018/4/13 20:29
//     */
//    private void orderRedeemSuccess(List<SyncOrderResponse> syncOrderResponseList, OrderRedeemAsyncBO orderRedeemAsyncBO) {
//        TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.SUCCESS;
//        BigDecimal transactionAmount = BigDecimal.ZERO;
//        BigDecimal transactionCharge = BigDecimal.ZERO;
//        for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
//            TransactionStatusEnum transactionStatusEnum_ = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
//            if (transactionStatusEnum_ == TransactionStatusEnum.SUCCESS) {
//                transactionAmount = transactionAmount.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
//                transactionCharge = transactionCharge.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
//            } else {
//                transactionStatusEnum = transactionStatusEnum_;
//                break;
//            }
//        }
//        if (transactionStatusEnum == TransactionStatusEnum.SUCCESS) {
//            //1.更新订单主表
//            orderInfoMapper.update(new OrderInfoDO(orderRedeemAsyncBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
//            //2.更新订单赎回表
//            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
//            orderRedeemDO.setTransactionStatus(TransactionStatusEnum.SUCCESS.getCode());
//            orderRedeemDO.setTransactionAmount(transactionAmount);
//            orderRedeemDO.setTransactionCharge(transactionCharge);
//            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
//            //全部赎回成功发送短信
//            this.orderRedeemSuccessMsg(orderRedeemAsyncBO.getUserId(), transactionAmount, transactionCharge);
//        }
//    }

    /**
     * @param orderRedeemAsyncBO
     * @param transactionAmount
     * @param transactionCharge
     * @return
     * @Description 全部赎回成功发送短信
     * @author lizengqiang
     * @date 2018/5/3 14:06
     */
    private void orderRedeemSuccessMsg(OrderRedeemAsyncBO orderRedeemAsyncBO, BigDecimal transactionAmount, BigDecimal transactionCharge) {
        try {
            if (!this.sendMsgOnlyOnce(orderRedeemAsyncBO.getOrderNo())) {
                return;
            }
            if (StringUtils.isEmpty(orderRedeemAsyncBO.getUserId())) {
                log.error("orderRedeemSuccessMsg-userId is null");
                return;
            }
            BigDecimal redeemAmount = transactionAmount.subtract(transactionCharge);
            StringBuffer msg = new StringBuffer();
            msg.append("【拾年保】亲爱的").append(getNickName(orderRedeemAsyncBO.getUserId()));
            msg.append("，您申请的").append(redeemAmount.toString());
            msg.append("元已赎回成功（已扣除基金赎回费").append(transactionCharge).append("元）。");
            log.info("orderRedeemSuccessMsg:{}", msg.toString());

            Map<String, String> param = Maps.newHashMap();
            param.put("nickName", getNickName(orderRedeemAsyncBO.getUserId()));
            param.put("redeemAmount", redeemAmount.toString());
            param.put("transactionCharge", transactionCharge.toString());

            SmsMessage smsMessage = SmsMessage.builder().mobile(this.getMobile(orderRedeemAsyncBO.getUserId())).confType(SmsConfTypeEnum.AUTO_REDEEM_ALL_PAY_SUCCESS).param(param).build();

            amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(), smsMessage);

        } catch (Exception e) {
            log.error("orderRedeemSuccessMsg is error,userId:{},transactionAmount:{},transactionCharge:{}", orderRedeemAsyncBO.getUserId(), transactionAmount, transactionCharge);
        }
    }

    /**
     * @param orderRedeemAsyncBO
     * @param transactionAmount
     * @return
     * @Description 全部赎回失败发送短信
     * @author lizengqiang
     * @date 2018/5/3 14:06
     */
    private void orderRedeemFailMsg(OrderRedeemAsyncBO orderRedeemAsyncBO, BigDecimal transactionAmount, Date sendTime) {
        try {
            if (!this.sendMsgOnlyOnce(orderRedeemAsyncBO.getOrderNo())) {
                return;
            }
            if (StringUtils.isEmpty(orderRedeemAsyncBO.getUserId())) {
                log.error("orderRedeemFailMsg-userId is null");
                return;
            }
            StringBuffer msg = new StringBuffer();
            msg.append("【拾年保】亲爱的").append(getNickName(orderRedeemAsyncBO.getUserId()));
            msg.append("，您于").append(DateTimeUtil.format(sendTime, DateTimeUtil.TimeFormat.CHINESE_SHORT_DATE_PATTERN));
            msg.append("申请的").append(transactionAmount.toString());
            msg.append("元赎回失败，可再次发起赎回，或拨打客服电话咨询:400-030-8585");
            log.info("orderRedeemFailMsg:{}", msg.toString());

            Map<String, String> param = Maps.newHashMap();
            param.put("nickName", getNickName(orderRedeemAsyncBO.getUserId()));
            param.put("sendTime", DateTimeUtil.format(sendTime, DateTimeUtil.TimeFormat.CHINESE_SHORT_DATE_PATTERN));
            param.put("transactionAmount", transactionAmount.toString());

            SmsMessage smsMessage = SmsMessage.builder().mobile(this.getMobile(orderRedeemAsyncBO.getUserId())).confType(SmsConfTypeEnum.AUTO_REDEEM_ALL_PAY_FAILED).param(param).build();

            amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(), smsMessage);
        } catch (Exception e) {
            log.error("orderRedeemFailMsg is error,userId:{},transactionAmount:{}", orderRedeemAsyncBO.getUserId(), transactionAmount);
        }
    }

    /**
     * @param transactionAmountSuccess
     * @param transactionChargeSuccess
     * @param transactionAmountFail
     * @param orderRedeemAsyncBO
     * @return
     * @Description 部分赎回成功到银行卡
     * @author lizengqiang
     * @date 2018/5/3 21:31
     */
    private void orderRedeemCompleteMsg(BigDecimal transactionAmountSuccess, BigDecimal transactionChargeSuccess, BigDecimal transactionAmountFail, OrderRedeemAsyncBO orderRedeemAsyncBO) {
        try {
            if (!this.sendMsgOnlyOnce(orderRedeemAsyncBO.getOrderNo())) {
                return;
            }
            if (StringUtils.isEmpty(orderRedeemAsyncBO.getUserId())) {
                log.error("orderRedeemCompleteMsg-userId is null");
                return;
            }
            BigDecimal totalAmount = transactionAmountSuccess.add(transactionAmountFail);
            StringBuffer msg = new StringBuffer();
            msg.append("【拾年保】亲爱的").append(getNickName(orderRedeemAsyncBO.getUserId()));
            msg.append("，您申请赎回的").append(totalAmount).append("元已完成确认，");
            msg.append(transactionAmountSuccess).append("元转出成（已扣除基金赎回费").append(transactionChargeSuccess).append("元）；");
            msg.append(transactionAmountFail).append("元赎回失败，仍在“拾年保”中。");
            log.info("orderRedeemCompleteMsg:{}", msg.toString());

            Map<String, String> param = Maps.newHashMap();
            param.put("nickName", getNickName(orderRedeemAsyncBO.getUserId()));
            param.put("totalAmount", totalAmount.toString());
            param.put("transactionAmountSuccess", transactionAmountSuccess.toString());
            param.put("transactionAmountFail", transactionAmountFail.toString());
            param.put("transactionChargeSuccess", transactionChargeSuccess.toString());

            SmsMessage smsMessage = SmsMessage.builder().mobile(this.getMobile(orderRedeemAsyncBO.getUserId())).confType(SmsConfTypeEnum.AUTO_REDEEM_PART_PAY_SUCCESS).param(param).build();

            amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(), smsMessage);
        } catch (Exception e) {
            log.error("orderRedeemCompleteMsg is error,orderRedeemAsyncBO:{}", orderRedeemAsyncBO.toString());
        }
    }

    private String getMobile(String userId) throws Exception {
        //查询手机号
        APIResponse<UserInfoResponse> apiResponse = userRemote.getUserInfo(userId);

        if (Objects.isNull(apiResponse) || !apiResponse.isSuccess()
                || Objects.isNull(apiResponse.getData())) {
            log.error("查询用户：{}信息失败", userId);
            throw new Exception("apiResponse is null，userId=" + userId);
        }
        String mobile = apiResponse.getData().getPhone();
        if (StringUtils.isEmpty(mobile)) {
            throw new Exception("apiResponse-mobile is null，userId=" + userId);
        }
        return mobile;
    }

    private String getNickName(String userId) {
        BaseFundRequest param = new BaseFundRequest();
        param.setFundPlatform(FundChannelEnum.YIFENG);
        param.setUserId(userId);
        APIResponse<GetUserFundAccountInfoResponse> userFundAccountInfo = null;
        try {
            userFundAccountInfo = fundUserRemote.getUserFundAccountInfo(param);
        } catch (BusinessException e) {
            log.error("获取用户基金账户信息系统异常,用户:{}", userId, e);
        }
        if (userFundAccountInfo == null || userFundAccountInfo.getData() == null || userFundAccountInfo.getData().getGender() == null) {
            return null;
        }

        String nickName = "用户";
        if (userFundAccountInfo.getData() == null
                || userFundAccountInfo.getData().getGender() == null || StringUtils.isEmpty(userFundAccountInfo.getData().getIdentityName())) {
            return nickName;
        }
        if (userFundAccountInfo.getData().getGender() == 1) {
            nickName = userFundAccountInfo.getData().getIdentityName().substring(0, 1) + "先生";
        } else if (userFundAccountInfo.getData().getGender() == 2) {
            nickName = userFundAccountInfo.getData().getIdentityName().substring(0, 1) + "女士";
        }
        return nickName;
    }

//    /**
//     * @param syncOrderResponseList
//     * @param orderRedeemAsyncBO
//     * @return
//     * @Description 当同步组合下的基金都为交易失败时更新订单主表和订单赎回表
//     * @author lizengqiang
//     * @date 2018/4/13 20:58
//     */
//    private void orderRedeemFail(List<SyncOrderResponse> syncOrderResponseList, OrderRedeemAsyncBO orderRedeemAsyncBO) {
//        BigDecimal transactionAmount = BigDecimal.ZERO;
//        BigDecimal transactionCharge = BigDecimal.ZERO;
//        int count = syncOrderResponseList.size();
//        for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {
//            TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
//            if (transactionStatusEnum == TransactionStatusEnum.FAIL || transactionStatusEnum == TransactionStatusEnum.PAY_FAIL) {
//                count--;
//                transactionAmount = transactionAmount.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
//                transactionCharge = transactionCharge.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
//            }
//        }
//        if (count == 0) {
//            //1.更新订单主表
//            orderInfoMapper.update(new OrderInfoDO(orderRedeemAsyncBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
//            //2.更新订单赎回表
//            OrderRedeemDO orderRedeemDO = new OrderRedeemDO(orderRedeemAsyncBO.getOrderNo());
//            orderRedeemDO.setTransactionStatus(TransactionStatusEnum.FAIL.getCode());
//            orderRedeemDO.setTransactionAmount(transactionAmount);
//            orderRedeemDO.setTransactionCharge(transactionCharge);
//            orderRedeemMapper.updateByOrderNo(orderRedeemDO);
//            //全部赎回失败发送短信
//            this.orderRedeemFailMsg(orderRedeemAsyncBO.getUserId(), transactionAmount, orderRedeemAsyncBO.getCreateTime());
//        }
//    }


    @Override
    public PageInfo<OrderRedeemAdminBO> pageOrderRedeem(OrderRedemptionListRequest request) {
        PageHelper.startPage(request.getPage(), request.getPageSize(), true);
        List<OrderRedeemAdminBO> list = orderRedeemMapper.listByOrderRedeemListCondition(request);
        return new PageInfo<>(list);
    }

    @Override
    public List<OrderRedeemDetailDO> listOrderRedeemDetail(OrderRedemptionDetailRequest request) {
        return orderRedeemDetailMapper.listOrderRedeemDetail(request);
    }

    @Override
    public OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO) {
        try {
            OrderInfoListBO orderInfoListBO = new OrderInfoListBO();
            OrderRedeemDO orderRedeemDO = orderRedeemMapper.queryByOrderNo(orderInfoDO.getOrderNo());
            Preconditions.checkNotNull(orderRedeemDO);
            //拷贝银行卡号，银行卡名称，银行卡logo
            BeanUtils.copyProperties(orderListBO, orderInfoListBO);
            //拷贝交易金额，交易费用
            if((orderRedeemDO.getTransactionCharge() == null || orderRedeemDO.getTransactionCharge().compareTo(BigDecimal.ZERO) == 0)
                    && orderInfoDO.getOrderStatus() == OrderStatusEnum.PROCESS.getCode()){
                //买入申请完成后无手续费,发送查询订单请求
                SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRedeemDO.getAccountNumber(),
                        orderRedeemDO.getMerchantNumber());
                log.info("{}赎回申请完成后查询订单详情,计算赎回费用：" + JSONObject.toJSONString(syncOrderRequest));

                BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>)
                        fundPortfolioService.syncOrder(syncOrderRequest);
                if (baseResponse.success()) {
                    SyncOrderListResponse syncOrderListResponse = baseResponse.getData();
                    // 返回的订单列表
                    List<SyncOrderResponse> syncOrderResponseList = syncOrderListResponse.getData();
                    BigDecimal transactionCharge = BigDecimal.ZERO;
                    if (!CollectionUtils.isEmpty(syncOrderResponseList)) {
                        for (SyncOrderResponse syncOrderResponse:syncOrderResponseList) {
                            transactionCharge = transactionCharge.add(new BigDecimal(syncOrderResponse.getTransactionCharge() == null ? "0.00" : syncOrderResponse.getTransactionCharge()));
                        }
                    }
                    orderRedeemDO.setTransactionCharge(transactionCharge);
                } else {
                    log.error("{}赎回申请完成后查询订单详情,计算赎回费用异常：" + JSONObject.toJSONString(syncOrderRequest));
                }
            }
            BeanUtils.copyProperties(orderRedeemDO, orderInfoListBO);
            //设置下单时间
            orderInfoListBO.setSendTime(orderInfoDO.getSendTime());
            //设置交易类型
            orderInfoListBO.setBusinessCode(orderInfoDO.getBusinessCode());
            //设置交易时间
            orderInfoListBO.setTransactionDate(orderInfoDO.getSendTime());
            this.buildOrderRedeemInfo(orderInfoListBO, orderInfoDO, orderRedeemDO);
            return orderInfoListBO;
        } catch (Exception e) {
            log.error("getOrderList is error,orderInfoDO:{},orderListBO:{}", orderInfoDO.toString(), orderListBO.toString(), e);
        }
        return null;
    }

    private OrderInfoListBO buildOrderRedeemInfo(OrderInfoListBO orderInfoListBO, OrderInfoDO orderInfoDO, OrderRedeemDO orderRedeemDO) {
        //先给一个预判断值，设置订单状态
        if (orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.PROCESS.getCode()) {
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_SUCCESS.getCode());
        } else if (orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.APPLY_FAIL.getCode()) {
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_FAIL.getCode());
            orderInfoListBO.setPricedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setCompletedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
        } else if (orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.COMPLETE.getCode()) {
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMPLETE.getCode());
        }
        List<OrderRedeemDetailDO> orderRedeemDetailList = orderRedeemDetailMapper.queryByRedeemId(orderRedeemDO.getOrderRedeemId());
        if (CollectionUtils.isEmpty(orderRedeemDetailList)) {
            return orderInfoListBO;
        }

        //发起赎回失败
        /*if(orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.APPLY_FAIL.getCode()){
            //取sendTime,手续费为0
            orderInfoListBO.setPricedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setCompletedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_FAIL.getCode());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            return orderInfoListBO;
        }*/

        //下单成功后,设置预期确认份额时间和赎回完成时间(+1),预计手续费,实际赎回金额
        List<OrderRedeemDetailDO> applySuccessOrderRedeemDetail = this.bulidApplySuccessOrderRedeemDetail(orderRedeemDetailList);
        if(CollectionUtils.isNotEmpty(applySuccessOrderRedeemDetail)){
            applySuccessOrderRedeemDetail.sort((o1,o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
            orderInfoListBO.setPricedDate(applySuccessOrderRedeemDetail.get(applySuccessOrderRedeemDetail.size() - 1).getExpectedConfirmedDate());
            orderInfoListBO.setCompletedDate(DateUtil.addDays(applySuccessOrderRedeemDetail.get(applySuccessOrderRedeemDetail.size() - 1).getExpectedConfirmedDate(),1));
            BigDecimal actualTransactionAmount = BigDecimal.ZERO;
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (OrderRedeemDetailDO orderRedeemDetailDO : applySuccessOrderRedeemDetail){
                actualTransactionAmount = actualTransactionAmount.add(orderRedeemDetailDO.getTransactionAmount());
                transactionCharge = transactionCharge.add(orderRedeemDetailDO.getDiscountTransactionCharge());
            }
            orderInfoListBO.setActualTransactionAmount(actualTransactionAmount);
            orderInfoListBO.setTransactionCharge(transactionCharge);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_SUCCESS.getCode());
        }

        int notFailCount = 0;
        for (OrderRedeemDetailDO orderRedeemDetaiDO : orderRedeemDetailList){
            if(orderRedeemDetaiDO.getTransactionStatus().intValue() != TransactionStatusEnum.FAIL.getCode()){
                notFailCount++;
            }
        }

        //设置确认份额的订单状态，交易时间，预计确认时间
        List<OrderRedeemDetailDO> pricedOrderRedeemDetail = this.buildPricedOrderRedeemDetail(orderRedeemDetailList);
        if (CollectionUtils.isNotEmpty(pricedOrderRedeemDetail) && pricedOrderRedeemDetail.size() == notFailCount) {
            pricedOrderRedeemDetail.sort((OrderRedeemDetailDO o1, OrderRedeemDetailDO o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
            //设置交易日期，设置预计确认时间，设置订单状态
//            orderInfoListBO.setTransactionDate(pricedOrderRedeemDetail.get(0).getPricedDate());
//            orderInfoListBO.setExpectPricedDate(pricedOrderRedeemDetail.get(pricedOrderRedeemDetail.size() - 1).getPricedDate());
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICE.getCode());
            //设置确认份额时间，订单状态,实际手续费
            orderInfoListBO.setPricedDate(pricedOrderRedeemDetail.get(pricedOrderRedeemDetail.size()-1).getPricedDate());
            orderInfoListBO.setCompletedDate(DateUtil.addDays(pricedOrderRedeemDetail.get(pricedOrderRedeemDetail.size() - 1).getExpectedConfirmedDate(),1));
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICED.getCode());
            BigDecimal actualTransactionAmount = BigDecimal.ZERO;
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (OrderRedeemDetailDO orderRedeemDetailDO : pricedOrderRedeemDetail){
                actualTransactionAmount = actualTransactionAmount.add(orderRedeemDetailDO.getTransactionAmount());
                transactionCharge = transactionCharge.add(orderRedeemDetailDO.getDiscountTransactionCharge());
            }
            orderInfoListBO.setActualTransactionAmount(actualTransactionAmount);
            orderInfoListBO.setTransactionCharge(transactionCharge);
        }

        //设置完成中的完成时间，设置订单状态
        List<OrderRedeemDetailDO> buildCompletedOrderRedeem = this.buildCompletedOrderRedeemDetail(orderRedeemDetailList);
        if (CollectionUtils.isNotEmpty(buildCompletedOrderRedeem) && buildCompletedOrderRedeem.size() == notFailCount) {
            buildCompletedOrderRedeem.sort((OrderRedeemDetailDO o1, OrderRedeemDetailDO o2) -> o1.getCompletedDate().compareTo(o2.getCompletedDate()));
            //设置完成时间，设置订单状态
           // orderInfoListBO.setCompletedDate(buildCompletedOrderRedeem.get(buildCompletedOrderRedeem.size() - 1).getCompletedDate());
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMPLETE.getCode());
        }

        //详情都为失败则失败,手续费为0
        List<OrderRedeemDetailDO> buildFailedOrderRedeem = this.buildFailedOrderRedeemDetail(orderRedeemDetailList);
        if (CollectionUtils.isNotEmpty(buildFailedOrderRedeem) && buildFailedOrderRedeem.size() ==  orderRedeemDetailList.size()) {
            buildFailedOrderRedeem.sort(( o1, o2) -> o1.getVoidDate().compareTo(o2.getVoidDate()));
            orderInfoListBO.setPricedDate(buildFailedOrderRedeem.get(buildFailedOrderRedeem.size() - 1).getVoidDate());
            orderInfoListBO.setCompletedDate(buildFailedOrderRedeem.get(buildFailedOrderRedeem.size() - 1).getVoidDate());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
        }

        //如果详情都为失败状态，手续费为0
//        if (this.orderRedeemDetailAllFail(buildCompletedOrderRedeem)) {
//            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
//        }
        return orderInfoListBO;
    }

    private List<OrderRedeemDetailDO> buildFailedOrderRedeemDetail(List<OrderRedeemDetailDO> orderRedeemDetailList) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailList) {
            Integer transactionStatus = orderRedeemDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date voidDate = orderRedeemDetailDO.getVoidDate();
                if (voidDate != null) {
                    result.add(orderRedeemDetailDO);
                }
            }else {
                return null;
            }
        }
        return result;

    }

    private List<OrderRedeemDetailDO> bulidApplySuccessOrderRedeemDetail(List<OrderRedeemDetailDO> orderRedeemDetailList) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailList) {
            Integer transactionStatus = orderRedeemDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            if (transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.RECEIVING.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()) {
                Date expectedConfirmedDate = orderRedeemDetailDO.getExpectedConfirmedDate();
                if (expectedConfirmedDate != null) {
                    result.add(orderRedeemDetailDO);
                }
            }else {
                return null;
            }
        }
        return result;

    }

    private Boolean orderRedeemDetailAllFail(List<OrderRedeemDetailDO> orderRedeemDetailList) {
        if (CollectionUtils.isEmpty(orderRedeemDetailList)) {
            return false;
        }
        Boolean flag = true;
        for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailList) {
            Integer transactionStatus = orderRedeemDetailDO.getTransactionStatus();
            if (transactionStatus == null || transactionStatus.intValue() != TransactionStatusEnum.FAIL.getCode()) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    private List<OrderRedeemDetailDO> buildPricedOrderRedeemDetail(List<OrderRedeemDetailDO> orderRedeemDetailList) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailList) {
            Integer transactionStatus = orderRedeemDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                //continue;
                return null;
            }
            //transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode() ||
            if (transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
//                    || transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date pricedDate = orderRedeemDetailDO.getPricedDate();
                if (pricedDate != null) {
                    result.add(orderRedeemDetailDO);
                }
            }
        }
        return result;
    }

    /**
     * @param orderRedeemDetailList
     * @return
     * @Description 只要有1比基金不是完成的就返回空
     * @author lizengqiang
     * @date 2018/4/28 18:39
     */
    private List<OrderRedeemDetailDO> buildCompletedOrderRedeemDetail(List<OrderRedeemDetailDO> orderRedeemDetailList) {
        List<OrderRedeemDetailDO> result = Lists.newArrayList();
        for (OrderRedeemDetailDO orderRedeemDetailDO : orderRedeemDetailList) {
            Integer transactionStatus = orderRedeemDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            // || transactionStatus.intValue() == TransactionStatusEnum.CANCELED.getCode()
            if (transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date completedDate = orderRedeemDetailDO.getCompletedDate();
                if (completedDate != null) {
                    result.add(orderRedeemDetailDO);
                }
            }/* else if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date completedDate = orderRedeemDetailDO.getVoidDate();
                if (completedDate != null) {
                    orderRedeemDetailDO.setCompletedDate(completedDate);
                    result.add(orderRedeemDetailDO);
                }
            } else if (transactionStatus.intValue() == TransactionStatusEnum.PAY_FAIL.getCode()) {
                Date completedDate = orderRedeemDetailDO.getCreateTime();
                orderRedeemDetailDO.setCompletedDate(completedDate);
                if (completedDate != null) {
                    result.add(orderRedeemDetailDO);
                }
            } else {
                return null;
            }*/
        }
        return result;
    }

    @Override
    public OrderRedeemFeeResponse orderRedeemFee(OrderRedeemBO orderRedeemBO) throws Exception {
        YfRedeemPortfolioFeeRequest redeemPortfolioFeeRequest = new YfRedeemPortfolioFeeRequest();
        redeemPortfolioFeeRequest.setPortfolioId(Long.parseLong(orderRedeemBO.getPortfolioId()));
        redeemPortfolioFeeRequest.setAccountNumber(orderRedeemBO.getAccountNumber());
        redeemPortfolioFeeRequest.setRedemptionAmount(orderRedeemBO.getTransactionAmount());
        BaseResponse<YfRedeemPortfolioFeeResponse> result = (BaseResponse<YfRedeemPortfolioFeeResponse>) fundPortfolioService.queryPortfolioRedeemFee(redeemPortfolioFeeRequest);
        if (result.success() && result.getData() != null) {
            YfRedeemPortfolioFeeResponse redeemPortfolioFeeResponse = result.getData();
            OrderRedeemFeeResponse orderRedeemFeeResponse = new OrderRedeemFeeResponse();
            orderRedeemFeeResponse.setTotalRedemptionCharge(redeemPortfolioFeeResponse.getTotalRedemptionCharge());
//            FundMainModelDetailRequest fundMainModelDetailRequest = new FundMainModelDetailRequest();
//            fundMainModelDetailRequest.setThirdPortfolioCode(orderRedeemBO.getPortfolioCode());
//            BigDecimal feeRate = orderInfoService.calculateFeeRate(fundMainModelDetailRequest, this.buildOrderFeeList(redeemPortfolioFeeResponse.getFeeDetails()));
            BigDecimal feeRate = redeemPortfolioFeeResponse.getTotalRedemptionCharge().divide(orderRedeemBO.getTransactionAmount(), 4, RoundingMode.HALF_UP).
                    multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
            orderRedeemFeeResponse.setFeeRate(feeRate);
            return orderRedeemFeeResponse;
        }
        return null;
    }

    @Override
    public List<OrderRedeemDO> queryByMerchantNumber(String merchantNumber, Integer channel) throws Exception {
        if (StringUtils.isEmpty(merchantNumber) || channel == null) {
            throw new Exception("merchantNumber or channel is null");
        }
        return orderRedeemMapper.queryByMerchantNumber(merchantNumber, channel);
    }

    @Override
    public String getExpectedConfirmedDate(Long mainModelId) throws Exception {
        String expectedConfirmedDate = StringUtils.EMPTY;
        FundMainModelDetailRequest request = new FundMainModelDetailRequest();
        request.setFundMainModelId(mainModelId);
        APIResponse<List<FundMainModelDetailDTO>> listAPIResponse = fundMainModelRemote.queryFundMainModelDetailByCondition(request);
        if (!listAPIResponse.isSuccess()) {
            log.info("获取基金明细失败,主理人模型id:{},原因:{}", mainModelId, listAPIResponse.getMsg());
            return expectedConfirmedDate;
        }
        List<FundMainModelDetailDTO> data = listAPIResponse.getData();
        if (CollectionUtils.isEmpty(data)) {
            log.info("获取到基金明细为空,主理人模型id:{}", mainModelId);
            return expectedConfirmedDate;
        }

        StringBuilder fundCodeBuffer = new StringBuilder();
        for (FundMainModelDetailDTO fundMainModelDetailDTO : data) {
            if (StringUtils.isNotEmpty(fundMainModelDetailDTO.getFundCode())) {
                fundCodeBuffer.append(fundMainModelDetailDTO.getFundCode()).append(",");
            }
        }
        String fundCodeStr = fundCodeBuffer.toString();
        if (StringUtils.isEmpty(fundCodeStr) || fundCodeStr.length() < 2) {
            return expectedConfirmedDate;
        }
        YfRedeemExpectedConfirmedDateRequest redeemExpectedConfirmedDateRequest = new YfRedeemExpectedConfirmedDateRequest();
        redeemExpectedConfirmedDateRequest.setFundCodes(fundCodeStr.substring(0, fundCodeStr.length() - 1));
        BaseResponse<YfRedeemExpectedConfirmedDateResponse> redeemExpectedConfirmedDateResponse = (BaseResponse<YfRedeemExpectedConfirmedDateResponse>) fundPortfolioService.queryRedeemExpectedConfirmedDate(redeemExpectedConfirmedDateRequest);
        if (redeemExpectedConfirmedDateResponse.success() && redeemExpectedConfirmedDateResponse.getData() != null) {
            expectedConfirmedDate = StringUtils.defaultString(redeemExpectedConfirmedDateResponse.getData().getExpectedCofirmedDate());
        }
        return expectedConfirmedDate;
    }

    @Transactional
    @Override
    public void createForceOrderRedeem(ForceOrderBO forceOrderBO) throws Exception {
        log.info("创建强制赎回订单开始-forceOrderBO:{}", forceOrderBO.toString());
        if (StringUtils.isEmpty(forceOrderBO.getMerchantNumber())) {
            log.info("强制赎回订单商户订单流水号为空-forceOrderBO:{}", forceOrderBO.toString());
            return;
        }
        List<OrderRedeemDO> orderRedeemDOList = orderRedeemMapper.queryByMerchantNum(forceOrderBO.getMerchantNumber(), FundChannelEnum.YIFENG.getChannel());
        if (CollectionUtils.isNotEmpty(orderRedeemDOList)) {
            log.info("强制赎回订单已经被处理过-forceOrderBO:{}", forceOrderBO.toString());
            return;
        }
        //插入订单主表
        OrderInfoDO orderInfoDO = OrderInfoDO.build(OrderInfoDO.OrderInfoBuildEnum.FORCE, OrderBusinessEnum.FORCE_REDEEM);
        orderInfoDO.setOrderNo(guidCreater.getUniqueID());
        orderInfoDO.setUserId(forceOrderBO.getUserId());
        orderInfoDO.setTransactionAmount(this.formatDecimal(forceOrderBO.getTransactionAmount()));
        orderInfoMapper.insert(orderInfoDO);
        log.info("创建强制赎回订单主订单-orderInfoDO:{}", orderInfoDO.toString());
        //插入订单赎回表
        OrderRedeemDO orderRedeemDO = OrderRedeemDO.build(OrderRedeemDO.OrderRedeemBuildEnum.FORCE);
        orderRedeemDO.setOrderRedeemId(guidCreater.getUniqueID());
        orderRedeemDO.setMerchantNumber(StringUtils.defaultString(forceOrderBO.getMerchantNumber()));
        orderRedeemDO.setOrderNo(orderInfoDO.getOrderNo());
        orderRedeemDO.setTransactionAmount(orderInfoDO.getTransactionAmount());
        orderRedeemDO.setUserId(orderInfoDO.getUserId());
        orderRedeemDO.setAccountNumber(StringUtils.defaultString(forceOrderBO.getAccountNumber()));
        orderRedeemDO.setPortfolioId(StringUtils.defaultString(forceOrderBO.getPortfolioId()));
        orderRedeemDO.setPortfolioCode(StringUtils.defaultString(forceOrderBO.getPortfolioCode()));
        orderRedeemDO.setInvestorPayId(0);
        TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, forceOrderBO.getTransactionStatus());
        orderRedeemDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
        orderRedeemMapper.insert(orderRedeemDO);
        log.info("创建强制赎回订单-orderRedeemDO:{}", orderRedeemDO.toString());
        //插入订单赎回详情表
        OrderRedeemDetailDO orderRedeemDetailDO = new OrderRedeemDetailDO();
        orderRedeemDetailDO.setOrderRedeemDetailId(guidCreater.getUniqueID());
        orderRedeemDetailDO.setOrderRedeemId(orderRedeemDO.getOrderRedeemId());
        orderRedeemDetailDO.setUserId(StringUtils.defaultString(orderInfoDO.getUserId()));
        orderRedeemDetailDO.setAccountNumber(StringUtils.defaultString(orderRedeemDO.getAccountNumber()));
        orderRedeemDetailDO.setBankCode(StringUtils.EMPTY);
        orderRedeemDetailDO.setBankNumber(StringUtils.EMPTY);
        orderRedeemDetailDO.setCancelEnable(StringUtils.defaultString(forceOrderBO.getCancelable()));
        orderRedeemDetailDO.setCanceledDate(DateTimeUtil.parseDate((forceOrderBO.getCanceledDate())));
        orderRedeemDetailDO.setCompletedDate(null);
        orderRedeemDetailDO.setContractNumber(StringUtils.EMPTY);
        orderRedeemDetailDO.setDiscountRate(BigDecimal.ZERO);
        orderRedeemDetailDO.setDiscountTransactionCharge(BigDecimal.ZERO);
        orderRedeemDetailDO.setExpectedConfirmedDate(null);
        orderRedeemDetailDO.setFundCode(StringUtils.defaultString(forceOrderBO.getFundCode()));
        orderRedeemDetailDO.setFundName(StringUtils.defaultString(forceOrderBO.getFundName()));
        orderRedeemDetailDO.setInvestorPayId(forceOrderBO.getInvestorPayId());
        orderRedeemDetailDO.setMerchantNumber(StringUtils.defaultString(forceOrderBO.getMerchantNumber()));
        orderRedeemDetailDO.setOrderDate(DateTimeUtil.parseDate(forceOrderBO.getOrderDate()));
        orderRedeemDetailDO.setPayMethod(orderRedeemDO.getPaymentMethod());
        orderRedeemDetailDO.setPortfolioId(StringUtils.defaultString(forceOrderBO.getPortfolioId()));
        orderRedeemDetailDO.setPricedDate(DateTimeUtil.parseDate((forceOrderBO.getPricedDate())));
        orderRedeemDetailDO.setReason(StringUtils.EMPTY);
        orderRedeemDetailDO.setRspId(null);
        orderRedeemDetailDO.setSettlementDate(null);
        orderRedeemDetailDO.setTransactionAmount(this.formatDecimal(forceOrderBO.getTransactionAmount()));
        orderRedeemDetailDO.setTransactionCharge(this.formatDecimal(forceOrderBO.getTransactionCharge()));
        orderRedeemDetailDO.setTransactionDate(DateTimeUtil.parseDate((forceOrderBO.getTransactionDate())));
        orderRedeemDetailDO.setTransactionPrice(BigDecimal.ZERO);
        orderRedeemDetailDO.setTransactionRate(this.formatDecimal(forceOrderBO.getTransactionRate()));
        orderRedeemDetailDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
        orderRedeemDetailDO.setTransactionType(OrderBusinessEnum.FORCE_REDEEM.getCode());
        orderRedeemDetailDO.setTransactionUnit(this.formatDecimal(forceOrderBO.getTransactionUnit()));
        orderRedeemDetailDO.setVoidDate(null);
        orderRedeemDetailDO.setExpectedDealDate(null);
        orderRedeemDetailDO.setPortfolioCode(StringUtils.defaultString(forceOrderBO.getPortfolioCode()));
        orderRedeemDetailMapper.insert(orderRedeemDetailDO);
        log.info("创建强制赎回订单详情-orderRedeemDetailDO:{}", orderRedeemDO.toString());
    }

    @Override
    public void syncOrderRedeem(OrderRedeemAsyncBO orderRedeemAsyncBO) throws Exception {
        //发送查询订单请求
        SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRedeemAsyncBO.getAccountNumber(), orderRedeemAsyncBO.getMerchantNumber());
        log.info("发送查询订单请求:{}", syncOrderRequest.toString());
        BaseResponse<SyncOrderListResponse> baseResponse = (BaseResponse<SyncOrderListResponse>) fundPortfolioService.syncOrder(syncOrderRequest);
        log.info("发送查询订单响应:{}", baseResponse.toString());
        this.syncOrderRedeem(orderRedeemAsyncBO, baseResponse);
    }


    private BigDecimal formatDecimal(String b) {
        if (StringUtils.isEmpty(b)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(b);
        } catch (Exception e) {
            log.error("formatDecimal is error,b:{}", b, e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal formatDecimal(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    private BigDecimal formatNull(String param) {
        if (StringUtils.isEmpty(param)) {
            return null;
        }
        try {
            return new BigDecimal(param);
        } catch (Exception e) {
            log.error("formatNull is error,param:{}", param, e);
        }
        return null;
    }
}
