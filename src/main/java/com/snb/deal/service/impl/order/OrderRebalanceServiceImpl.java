package com.snb.deal.service.impl.order;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.datetime.DateUtil;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.RebalanceOrderMessage;
import com.snb.deal.admin.api.dto.order.OrderRebalanceDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderRebalanceListRequest;
import com.snb.deal.api.dto.rebalance.RebalanceAuthResponse;
import com.snb.deal.api.dto.rebalance.RebalanceConfirmResponse;
import com.snb.deal.api.dto.rebalance.RebalanceRequest;
import com.snb.deal.api.enums.order.TransactionTypeEnum;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.bo.order.OrderRebalanceAdminBO;
import com.snb.deal.bo.order.OrderRebalanceDetailBO;
import com.snb.deal.bo.rebalance.*;
import com.snb.deal.entity.order.*;
import com.snb.deal.enums.*;
import com.snb.deal.mapper.order.*;
import com.snb.deal.service.order.OrderRebalanceFailureRecordService;
import com.snb.deal.service.order.OrderRebalanceService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import com.snb.third.yifeng.dto.order.SyncOrderResponse;
import com.snb.third.yifeng.dto.order.rebalance.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class OrderRebalanceServiceImpl implements OrderRebalanceService {

    @Resource
    private OrderRebalanceMapper orderRebalanceMapper;

    @Resource
    private OrderRebalanceDetailMapper orderRebalanceDetailMapper;

    @Resource
    GuidCreater guidCreater;

    @Resource
    OrderInfoMapper orderInfoMapper;

    @Resource
    OrderRebalanceAuthMapper orderRebalanceAuthMapper;

    @Resource
    OrderRebalanceSummaryMapper orderRebalanceSummaryMapper;

    @Resource
    OrderRebalanceSummaryDetailMapper orderRebalanceSummaryDetailMapper;

    @Resource
    private FundRebalanceBiz fundRebalanceBiz;
    @Resource
    OrderRebalanceService rebalanceService;
    @Resource
    OrderRebalanceFailureRecordService orderRebalanceFailureRecordService;
    @Resource
    Environment environment;
    @Resource
    FundPortfolioService fundPortfolioService;

    @Override
    public PageInfo<OrderRebalanceAdminBO> pageOrderRebalance(OrderRebalanceListRequest request) {
        PageHelper.startPage(request.getPage(), request.getPageSize(), true);
        List<OrderRebalanceAdminBO> list = orderRebalanceMapper.listByOrderRebalanceCondition(request);
        for (OrderRebalanceAdminBO orderRebalanceAdminBO : list) {
            orderRebalanceAdminBO.setOrderRebalanceResult(OrderStatusEnum.getOrderStatus(orderRebalanceAdminBO.getTransactionStatus()).getDesc());
        }
        return new PageInfo<>(list);
    }


    @Override
    public List<OrderRebalanceDetailBO> listOrderRebalanceDetail(OrderRebalanceDetailRequest condition) {
        List<OrderRebalanceDetailBO> list = orderRebalanceDetailMapper.listByOrderRebalanceDetailCondition(condition);
        if (CollectionUtils.isNotEmpty(list)) {
            for (OrderRebalanceDetailBO orderRebalanceDetailBO : list) {
                orderRebalanceDetailBO.setTransactionStatusResult(TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, orderRebalanceDetailBO.getTransactionStatus()).getDesc());
            }
        }
        return list;
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createOrderInfo(OrderRebalanceDO orderRebalanceDO) {
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setUserId(orderRebalanceDO.getUserId());
        orderInfoDO.setOrderNo(orderRebalanceDO.getOrderNo());
        orderInfoDO.setOrderStatus(OrderStatusEnum.PROCESS.getCode());
        orderInfoDO.setBusinessCode(OrderBusinessEnum.REBALANCE.getCode());
        orderInfoDO.setSource(OrderSourceEnum.H5.getCode());
        orderInfoDO.setTransactionAmount(BigDecimal.ZERO);
        orderInfoDO.setChannel(orderRebalanceDO.getChannel());
        int r = orderInfoMapper.insert(orderInfoDO);
        if (r <= 0) {
            log.error("调仓新增订单信息失败={}", JSONObject.toJSONString(orderInfoDO));
            throw new RuntimeException("调仓新增订单信息失败=" + orderInfoDO.getOrderNo());
        }
    }


    @Override
    public List<OrderRebalanceAsyncBO> queryOrderRebalanceAsync(Integer channel, Integer businessCode, Integer transactionStatus,
                                                                Integer pagetIndex, Integer pageSize) {
        List<OrderRebalanceAsyncBO> orderRebalanceAsyncDtoList =
                orderRebalanceMapper.queryOrderRebalanceAsync(channel, businessCode, transactionStatus, pagetIndex, pageSize);
        return orderRebalanceAsyncDtoList;
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void syncOrderRebalance(OrderRebalanceAsyncBO orad, List<OrderRebalanceDetailDO> orderRebalanceDetailList) throws Exception {

        int successNum = 0;
        int failureNum = 0;
        int partStatusNum = 0;
        int total = orderRebalanceDetailList.size();

        //批量更新子订单信息
        orderRebalanceDetailMapper.batchUpdateRebalanceDetail(orderRebalanceDetailList);
        // 交易费用
        BigDecimal transactionCharge = BigDecimal.ZERO;
        for (OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList) {

            // 成功
            if (TransactionStatusEnum.SUCCESS.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                transactionCharge = transactionCharge.add(orderRebalanceDetailDO.getDiscountTransactionCharge());
                successNum++;
                // 失败
            } else if (TransactionStatusEnum.FAIL.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                failureNum++;
            } else {
                // 不明确的状态
                partStatusNum++;
            }
        }


        Integer orderStatus = OrderStatusEnum.PROCESS.getCode();
        Integer transactionStatus = OrderStatusEnum.PROCESS.getCode();
        // 所有成功
        if (successNum == total) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.SUCCESS.getCode();
        } else if (failureNum == total) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.FAIL.getCode();
        } else if (successNum > 0 && failureNum > 0 && (successNum + failureNum == total)) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.SUCCESS_PART.getCode();
        }

        this.updateOrderInfo4Rebalance(orad.getUserId(), orad.getOrderNo(), transactionCharge, orad.getOrderRebanlanceId(), transactionStatus, orderStatus);

    }


    /**
     * 更新订单的信息-调仓
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-26
     */
    private void updateOrderInfo4Rebalance(String userId, Long orderNo, BigDecimal transactionCharge,
                                           Long orderRebalanceId, Integer transactionStatus, Integer orderStatus) throws Exception {
//        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
//        orderRebalanceConditionBO.setUserId(userId);
//        orderRebalanceConditionBO.setOrderRebalanceId(orderRebalanceId);
//        orderRebalanceConditionBO.setTransactionStatus(TransactionStatusEnum.SUCCESS.getCode());
//
//        // 查询调仓赎回子订单是否存有非交易成功的
//        Long count = orderRebalanceDetailMapper.existNoFinishOrder(orderRebalanceConditionBO);
        // 调仓赎回子订单和调仓申购子订单都交易成功
//        if (count == 0L) {
//            this.updateOrderRebalanceFeeAndStatus(orderRebalanceId,OrderStatusEnum.COMPLETE.getCode(),
//                    transactionCharge,orderNo);
//        } else {
//            if (transactionCharge.compareTo(BigDecimal.ZERO) > 0) {
//                this.updateTransactionCharge(orderRebalanceId,transactionCharge);
//            }
//        }


        if (OrderStatusEnum.COMPLETE.getCode() == orderStatus) {
            this.updateOrderRebalanceFeeAndStatus(orderRebalanceId, transactionStatus, orderStatus, transactionCharge, orderNo);
        } else {
            if (transactionCharge.compareTo(BigDecimal.ZERO) > 0) {
                this.updateTransactionCharge(orderRebalanceId, transactionCharge);
            }
        }
    }


    /**
     * 更新交易费用
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-26
     */
    private void updateTransactionCharge(Long orderRebanlanceId, BigDecimal transactionCharge) throws Exception {
        OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
        orderRebalanceDO.setOrderRebalanceId(orderRebanlanceId);
        orderRebalanceDO.setTransactionCharge(transactionCharge);
        int j = orderRebalanceMapper.updateOrderRebalanceBycondition(orderRebalanceDO);
        if (j <= 0) {
            log.error("更新调仓订交易费用失败：OrderRebanlanceId={},transactionCharge={}",
                    orderRebanlanceId, transactionCharge);
            throw new RuntimeException("更新调仓订交易费用失败：OrderRebanlanceId=" + orderRebanlanceId);
        }
    }

    /**
     * 更新订单交易费用&状态；订单中心状态
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-26
     */
    private void updateOrderRebalanceFeeAndStatus(Long orderRebanlanceId, Integer transactionStatus, Integer orderStatus,
                                                  BigDecimal transactionCharge, Long orderNo) throws Exception {
        // 更新调仓订单状态
        this.updateOrderRebalanceBycondition(orderRebanlanceId, transactionStatus, transactionCharge, orderNo);
        // 更新订单中心的订单状态
        this.updateOrderInfo4Rebalance(orderNo, orderStatus);
    }


    /**
     * 更新订单中心的订单状态
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void updateOrderInfo4Rebalance(Long orderNo, Integer orderStatus) throws Exception {
        // 更新订单中心的订单状态成功
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderNo(orderNo);
        orderInfoDO.setOrderStatus(orderStatus);
        orderInfoDO.setTransactionFinishTime(new Date());
        int k = orderInfoMapper.update(orderInfoDO);
        if (k <= 0) {
            log.error("更新订单中心的订单状态失败：OrderNo={}", orderNo);
            throw new RuntimeException("更新订单中心的订单状态失败：OrderNo=" + orderNo);
        }
    }


    /**
     * 更新调仓订单状态
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void updateOrderRebalanceBycondition(Long orderRebanlanceId, Integer transactionStatus,
                                                 BigDecimal transactionCharge, Long orderNo) throws Exception {
        OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
        orderRebalanceDO.setOrderRebalanceId(orderRebanlanceId);
        orderRebalanceDO.setTransactionStatus(transactionStatus);
        orderRebalanceDO.setTransactionCharge(transactionCharge);
        orderRebalanceDO.setOrderRebalanceId(orderRebanlanceId);

        // 更新调仓订单状态成功
        int r = orderRebalanceMapper.updateOrderRebalanceBycondition(orderRebalanceDO);
        if (r <= 0) {
            log.error("更新调仓订单状态失败：OrderRebanlanceId={}", orderRebanlanceId);
            throw new RuntimeException("更新调仓订单状态失败：OrderRebanlanceId=" + orderRebanlanceId);
        }
    }


    @Override
    public Long queryOrderRebalanceCountBycondition(OrderRebalanceConditionBO orderRebalanceConditionBO) {
        return orderRebalanceMapper.queryOrderRebalanceCountBycondition(orderRebalanceConditionBO);
    }

    @Override
    public List<OrderRebalanceSendBO> querySendOrderRebalanceList(Integer code, int startIndex, int count) {
        return orderRebalanceMapper.querySendOrderRebalanceList(code, startIndex, count);
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void rebalanceBeforeDeal(OrderRebalanceCreateBO orderRebalanceCreateBO,
                                    YfRebalanceSummaryResponse yfRebalanceSummaryResponse) throws Exception {

        OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
        orderRebalanceDO.setOrderRebalanceId(guidCreater.getUniqueID());
        orderRebalanceDO.setUserId(orderRebalanceCreateBO.getUserId());
        orderRebalanceDO.setOrderNo(guidCreater.getUniqueID());
        orderRebalanceDO.setAccountNumber(orderRebalanceCreateBO.getThirdAccountNumber());
        orderRebalanceDO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
        orderRebalanceDO.setMerchantNumber(orderRebalanceCreateBO.getMerchantNumber());
        orderRebalanceDO.setThirdPortfolioId(orderRebalanceCreateBO.getThirdPortfolioId());
        orderRebalanceDO.setPortfolioCode(orderRebalanceCreateBO.getThirdPortfolioCode());
        orderRebalanceDO.setResponseCode("");
        orderRebalanceDO.setResponseMessage("");
        orderRebalanceDO.setChannel(orderRebalanceCreateBO.getChannel());

        // 调仓之前相关订单作废
        this.rebalanceAbandon(orderRebalanceDO);
        // 创建调仓订单
        this.createOrderInfo(orderRebalanceDO);
        // 创建基金调仓订单信息
        this.createOrderRebalanceInfo(orderRebalanceDO);
        // 创建调仓授权信息
        this.createOrderRebalanceAuthInfo(orderRebalanceDO);
        // 创建调仓概要信息
        this.createOrderRebalanceSummaryInfo(yfRebalanceSummaryResponse, orderRebalanceDO);

    }


    /**
     * 调仓之前相关订单作废
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-05-03
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void rebalanceAbandon(OrderRebalanceDO orderRebalanceDO) throws Exception {
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderStatus(OrderStatusEnum.PROCESS.getCode());
        orderInfoDO.setUserId(orderRebalanceDO.getUserId());
        orderInfoDO.setBusinessCode(OrderBusinessEnum.REBALANCE.getCode());
        orderInfoDO.setChannel(orderRebalanceDO.getChannel());

        List<OrderInfoDO> orderInfoDOList = orderInfoMapper.query(orderInfoDO);
        for (OrderInfoDO oido : orderInfoDOList) {

            OrderRebalanceDO ordo = new OrderRebalanceDO();
            ordo.setOrderNo(oido.getOrderNo());
            OrderRebalanceDO orderRebalanceDO1Abandon = orderRebalanceMapper.queryByOrderNo(ordo);

            // 作废订单信息异常
            this.beforeOrderInfoAbandon(oido);
            // 作废调仓订单
            this.beforeOrderRebalanceAbandon(orderRebalanceDO1Abandon);
            // 作废调仓授权信息
            this.beforeOrderRebalanceAuthAbandon(orderRebalanceDO1Abandon);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void rebalanceStartFailure(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception {


        long orderRebanlanceId = orderRebalanceSendBO.getOrderRebalanceId();

        int transactionStatus = OrderStatusEnum.APPLY_FAIL.getCode();

        BigDecimal transactionCharge = BigDecimal.ZERO;

        long orderNo = orderRebalanceSendBO.getOrderNo();

        // 更新调仓订单状态
        this.updateOrderRebalanceBycondition(orderRebanlanceId, transactionStatus, transactionCharge, orderNo);
        // 更新订单中心的订单状态
        this.updateOrderInfo4Rebalance(orderNo, transactionStatus);

        OrderRebalanceAuthDO orderRebalanceAuthDO = new OrderRebalanceAuthDO();
        orderRebalanceAuthDO.setUserId(orderRebalanceSendBO.getUserId());
        orderRebalanceAuthDO.setOrderRebalanceId(orderRebalanceSendBO.getOrderRebalanceId());
        orderRebalanceAuthDO.setAuthStatus(OrderRebalanceAuthStatusEnum.END.getCode());
        // 更新调仓授权信息结束
        Long k = orderRebalanceAuthMapper.update(orderRebalanceAuthDO);
        if (k <= 0L) {
            log.error("更新调仓授权信息状态失败：OrderRebalanceId={}", orderRebalanceSendBO.getOrderRebalanceId());
            throw new RuntimeException("更新调仓授权信息状态失败：OrderRebalanceId=" + orderRebalanceSendBO.getOrderRebalanceId());
        }
    }

    @Override
    public OrderRebalanceAuthDO getLastUnProcessRebalanceAuthByUserId(String userId) {
        return orderRebalanceAuthMapper.selectLastUnProcessByUserId(userId);
    }

    @Override
    public OrderRebalanceDO getById(Long orderRebalanceId) {
        return orderRebalanceMapper.selectByOrderRebalanceId(orderRebalanceId);
    }

    /**
     * 作废订单信息异常
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-05-03
     */
    private void beforeOrderInfoAbandon(OrderInfoDO oido) {
        Long orderNo = oido.getOrderNo();
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderNo(orderNo);
        orderInfoDO.setOrderStatus(OrderStatusEnum.ABANDON.getCode());
        int k = orderInfoMapper.update(orderInfoDO);
        if (k <= 0) {
            log.error("作废订单信息异常：OrderNo={}", orderNo);
            throw new RuntimeException("作废订单信息异常:OrderNo=" + orderNo);
        }

    }


    /**
     * 作废当前之前的调仓授权信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-05-03
     */
    private void beforeOrderRebalanceAuthAbandon(OrderRebalanceDO orderRebalanceDO) {
        OrderRebalanceAuthDO orderRebalanceAuthDOAbandon = new OrderRebalanceAuthDO();
        orderRebalanceAuthDOAbandon.setOrderRebalanceId(orderRebalanceDO.getOrderRebalanceId());
        orderRebalanceAuthDOAbandon.setAuthStatus(OrderRebalanceAuthStatusEnum.ABANDON.getCode());
        Long i = orderRebalanceAuthMapper.update(orderRebalanceAuthDOAbandon);
        if (i <= 0L) {
            log.error("作废调仓授权信息失败={}", JSONObject.toJSONString(orderRebalanceDO));
            throw new RuntimeException("作废调仓授权信息失败=" + orderRebalanceDO.getOrderNo());
        }

    }


    /**
     * 作废当前日之前的调仓订单信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-05-03
     */
    private void beforeOrderRebalanceAbandon(OrderRebalanceDO orderRebalanceDO) throws Exception {
        OrderRebalanceDO orderRebalanceDOAbandon = new OrderRebalanceDO();
        orderRebalanceDOAbandon.setOrderNo(orderRebalanceDO.getOrderNo());
        orderRebalanceDOAbandon.setTransactionStatus(OrderStatusEnum.ABANDON.getCode());
        int i = orderRebalanceMapper.updateOrderRebalanceBycondition(orderRebalanceDOAbandon);
        if (i <= 0) {
            log.error("作废当前日之前的调仓订单信息失败={}", JSONObject.toJSONString(orderRebalanceDO));
            throw new RuntimeException("作废当前日之前的调仓订单信息失败=" + orderRebalanceDO.getOrderNo());
        }
    }


    /**
     * 创建调仓概要信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createOrderRebalanceSummaryInfo(YfRebalanceSummaryResponse yfRebalanceSummaryResponse, OrderRebalanceDO orderRebalanceDO) {
        Long orderRebalanceSummaryDOId = guidCreater.getUniqueID();
        // 创建调仓基本概要信息
        this.createOrderRebalanceSummaryBaseInfo(yfRebalanceSummaryResponse, orderRebalanceDO, orderRebalanceSummaryDOId);
        // 创建调仓概要明细信息
        this.createOrderRebalanceSummaryDetailInfo(yfRebalanceSummaryResponse, orderRebalanceDO, orderRebalanceSummaryDOId);

    }


    /**
     * 创建调仓概要明细信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createOrderRebalanceSummaryDetailInfo(YfRebalanceSummaryResponse yfRebalanceSummaryResponse,
                                                       OrderRebalanceDO orderRebalanceDO, Long orderRebalanceSummaryDOId) {

        // 新增调仓概要信息明细
        List<OrderRebalanceSummaryDetailDO> orderRebalanceSummaryDetailDOList = Lists.newArrayList();

        List<YfRebalanceDetailSummary> yfRebalanceDetailSummaryList = yfRebalanceSummaryResponse.getRebalanceDetails();
        List<YfRebalanceFeeDetailSummary> yfRebalanceFeeDetailSummaryList = yfRebalanceSummaryResponse.getFeeDetails();


        for (YfRebalanceDetailSummary yfRebalanceDetailSummary : yfRebalanceDetailSummaryList) {

            OrderRebalanceSummaryDetailDO orderRebalanceSummaryDetailDO = new OrderRebalanceSummaryDetailDO();
            orderRebalanceSummaryDetailDO.setOrderRebalanceSummaryDetailId(guidCreater.getUniqueID());
            orderRebalanceSummaryDetailDO.setOrderRebalanceSummaryId(orderRebalanceSummaryDOId);
            orderRebalanceSummaryDetailDO.setFundName(yfRebalanceDetailSummary.getFundName());
            orderRebalanceSummaryDetailDO.setFundCode(yfRebalanceDetailSummary.getFundCode());
            orderRebalanceSummaryDetailDO.setFundType(yfRebalanceDetailSummary.getType());

            // 解析交易类型
            if(null == yfRebalanceDetailSummary.getType()){
                continue;
            }
            OrderRebalanceTransactionTypeEnum rebalanceTransactionTypeEnum =
                    OrderRebalanceTransactionTypeEnum.getRebalanceTransactionTypeEnum(FundChannelEnum.YIFENG,
                            yfRebalanceDetailSummary.getType());

            orderRebalanceSummaryDetailDO.setTransactionType(rebalanceTransactionTypeEnum.getCode());

            // 调仓投资有交易金额
            if (OrderRebalanceTransactionTypeEnum.ORDERREBALANCE_INVEST.getCode().equals(rebalanceTransactionTypeEnum.getCode())) {
                orderRebalanceSummaryDetailDO.setInvestmentAmount(new BigDecimal(yfRebalanceDetailSummary.getInvestmentAmount()));
                orderRebalanceSummaryDetailDO.setInvestmentUnits(BigDecimal.ZERO);
            } else {
                orderRebalanceSummaryDetailDO.setInvestmentAmount(BigDecimal.ZERO);
                orderRebalanceSummaryDetailDO.setInvestmentUnits(new BigDecimal(yfRebalanceDetailSummary.getInvestmentUnits()));
            }

            orderRebalanceSummaryDetailDO.setPostProportion(new BigDecimal(yfRebalanceDetailSummary.getPostProportion()));
            orderRebalanceSummaryDetailDO.setPreProportion(new BigDecimal(yfRebalanceDetailSummary.getPreProportion()));
            orderRebalanceSummaryDetailDO.setUserId(orderRebalanceDO.getUserId());
            orderRebalanceSummaryDetailDO.setChannel(orderRebalanceDO.getChannel());

            for (YfRebalanceFeeDetailSummary yfRebalanceFeeDetailSummary : yfRebalanceFeeDetailSummaryList) {
                String fundCode = yfRebalanceFeeDetailSummary.getFundCode();
                if (yfRebalanceDetailSummary.getFundCode().equals(fundCode)) {
                    orderRebalanceSummaryDetailDO.setTransactionCharge(new BigDecimal(yfRebalanceFeeDetailSummary.getTransactionCharge()));
                    break;
                }
            }
            orderRebalanceSummaryDetailDOList.add(orderRebalanceSummaryDetailDO);
        }

        if (!CollectionUtils.isEmpty(orderRebalanceSummaryDetailDOList)) {
            orderRebalanceSummaryDetailMapper.batchInsert(orderRebalanceSummaryDetailDOList);
        } else {
            log.error("调仓新增概要明细信息失败:YfRebalanceSummaryResponse={},OrderRebalanceDO={},orderRebalanceSummaryDOId={}",
                    JSONObject.toJSONString(yfRebalanceSummaryResponse), JSONObject.toJSONString(orderRebalanceDO), orderRebalanceSummaryDOId);
            throw new RuntimeException("调仓新增概要明细信息失败");
        }
    }


    /**
     * 创建调仓基本概要信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createOrderRebalanceSummaryBaseInfo(YfRebalanceSummaryResponse yfRebalanceSummaryResponse,
                                                     OrderRebalanceDO orderRebalanceDO, Long orderRebalanceSummaryDOId) {
        OrderRebalanceSummaryDO orderRebalanceSummaryDO = new OrderRebalanceSummaryDO();
        orderRebalanceSummaryDO.setAbleReason(yfRebalanceSummaryResponse.getRebalanceAbleReason());
        orderRebalanceSummaryDO.setOrderRebalanceId(orderRebalanceDO.getOrderRebalanceId());
        orderRebalanceSummaryDO.setDisableReson(yfRebalanceSummaryResponse.getRebalanceDisableReson());
        orderRebalanceSummaryDO.setOrderRebalanceSummaryId(orderRebalanceSummaryDOId);
        orderRebalanceSummaryDO.setTotalFee(new BigDecimal(yfRebalanceSummaryResponse.getTotalRebalanceFee()));
        orderRebalanceSummaryDO.setUserId(orderRebalanceDO.getUserId());
        orderRebalanceSummaryDO.setChannel(orderRebalanceDO.getChannel());
        // 新增调仓概要信息
        orderRebalanceSummaryMapper.insert(orderRebalanceSummaryDO);
    }


    /**
     * 创建调仓授权信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createOrderRebalanceAuthInfo(OrderRebalanceDO orderRebalanceDO) {
        // 创建调仓授权信息
        OrderRebalanceAuthDO orderRebalanceAuthDO = new OrderRebalanceAuthDO();
        orderRebalanceAuthDO.setOrderRebalanceAuthId(guidCreater.getUniqueID());
        orderRebalanceAuthDO.setOrderRebalanceId(orderRebalanceDO.getOrderRebalanceId());
        orderRebalanceAuthDO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        orderRebalanceAuthDO.setUserId(orderRebalanceDO.getUserId());
        orderRebalanceAuthDO.setChannel(orderRebalanceDO.getChannel());

        int k = orderRebalanceAuthMapper.insert(orderRebalanceAuthDO);
        if (k <= 0) {
            log.error("调仓新增授权信息失败={}", JSONObject.toJSONString(orderRebalanceAuthDO));
            throw new RuntimeException("调仓新增授权信息失败=" + orderRebalanceDO.getOrderNo());
        }
    }


    /**
     * 创建基金调仓订单信息
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    private void createOrderRebalanceInfo(OrderRebalanceDO orderRebalanceDO) throws Exception {
        // 创建基金调仓订单信息
        int r = orderRebalanceMapper.createOrderRebalanceInfo(orderRebalanceDO);
        if (r <= 0) {
            log.error("调仓新增总订单信息失败={}", JSONObject.toJSONString(orderRebalanceDO));
            throw new RuntimeException("调仓新增订单信息失败=" + orderRebalanceDO.getOrderNo());
        }
    }


    @Event(reliability = true,
            eventType = "'rebalanceOrderApply'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.rebalance.apply",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void rebalanceAfterDeal(YfRebalancePortfolioResponse rpr, OrderRebalanceSendBO orderRebalanceSendBO) {
        List<YfRebalanceDetail> rebalanceDetailList = rpr.getRebalanceResult();

        List<OrderRebalanceDetailDO> orderRebalanceDetailList = new ArrayList<>();

        for (YfRebalanceDetail rd : rebalanceDetailList) {
            OrderRebalanceDetailDO ord = new OrderRebalanceDetailDO();
            ord.setOrderRebalanceDetailId(guidCreater.getUniqueID());
            ord.setUserId(orderRebalanceSendBO.getUserId());
            ord.setOrderRebalanceId(orderRebalanceSendBO.getOrderRebalanceId());
            ord.setContractNumber(rd.getContractNumber());
            ord.setFundCode(rd.getFundCode());
            ord.setFundName(rd.getFundName());
            ord.setInvestAmount(rd.getInvestmentAmount());
            ord.setOrderDate(DateTimeUtil.parseDate(rd.getOrderDate()));
            ord.setReason(rd.getReason());
            ord.setRedemptionUnit(rd.getRedemptionUnit());
            ord.setSettlementDate(DateTimeUtil.parseDate(rd.getSettlementDate()));
            ord.setTransactionCfmLagDay(Integer.parseInt(rd.getTransactionCfmLagDay()));
            ord.setTransactionDate(DateTimeUtil.parseDate(rd.getTransactionDate()));
            ord.setTransactionSettleLagDay(Integer.parseInt(rd.getTransactionSettleLagDay()));


            // 解析交易状态
            TransactionStatusEnum transactionStatusEnum =
                    TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, rd.getTransactionStatus());

            // 解析交易类型
            OrderRebalanceTransactionTypeEnum rebalanceTransactionTypeEnum =
                    OrderRebalanceTransactionTypeEnum.getRebalanceTransactionTypeEnum(FundChannelEnum.YIFENG, rd.getTransactionType());

            ord.setTransactionType(rebalanceTransactionTypeEnum.getCode());
            ord.setTransactionStatus(transactionStatusEnum.getCode());


            orderRebalanceDetailList.add(ord);
        }

        // 批量新增调仓明细信息
        orderRebalanceDetailMapper.batchAddRebalanceDetail(orderRebalanceDetailList);

        OrderRebalanceAuthDO orderRebalanceAuthDO = new OrderRebalanceAuthDO();
        orderRebalanceAuthDO.setUserId(orderRebalanceSendBO.getUserId());
        orderRebalanceAuthDO.setOrderRebalanceId(orderRebalanceSendBO.getOrderRebalanceId());
        orderRebalanceAuthDO.setAuthStatus(OrderRebalanceAuthStatusEnum.END.getCode());
        // 更新调仓授权信息结束
        Long k = orderRebalanceAuthMapper.update(orderRebalanceAuthDO);
        if (k <= 0L) {
            log.error("更新调仓授权信息状态失败：OrderRebalanceId={}", orderRebalanceSendBO.getOrderRebalanceId());
            throw new RuntimeException("更新调仓授权信息状态失败：OrderRebalanceId=" + orderRebalanceSendBO.getOrderRebalanceId());
        }

        //发送通知消息
        RebalanceOrderMessage rebalanceOrderMessage = new RebalanceOrderMessage();
        rebalanceOrderMessage.setUserId(orderRebalanceSendBO.getUserId());
        rebalanceOrderMessage.setAccountNumber(orderRebalanceSendBO.getThirdAccountNumber());
        rebalanceOrderMessage.setMerchantNumber(orderRebalanceSendBO.getMerchantNumber());
        rebalanceOrderMessage.setOrderNo(orderRebalanceSendBO.getOrderNo());
        rebalanceOrderMessage.setEventId(orderRebalanceSendBO.getUserId() + orderRebalanceSendBO.getOrderNo());
        EventMessageContext.addMessage(rebalanceOrderMessage);
    }

    @Override
    public OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO) {
        try {
            OrderInfoListBO orderInfoListBO = new OrderInfoListBO();
            OrderRebalanceDO orderRebalanceParam = new OrderRebalanceDO();
            orderRebalanceParam.setOrderNo(orderInfoDO.getOrderNo());
            OrderRebalanceDO orderRebalanceDO = orderRebalanceMapper.queryByOrderNo(orderRebalanceParam);
            Preconditions.checkNotNull(orderRebalanceDO);

            OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
            orderRebalanceAuthConditionBO.setOrderRebalanceId(orderRebalanceDO.getOrderRebalanceId());
            orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
            OrderRebalanceAuthDO orderRebalanceAuthDO = orderRebalanceAuthMapper.queryOrderRebalanceAuth(orderRebalanceAuthConditionBO);
            if (orderRebalanceAuthDO != null) {
                log.info("用户{}的调仓订单{}未授权完成不展示交易详情！OrderRebalanceAuthConditionBO={}", orderInfoDO.getUserId(),
                        orderRebalanceDO.getOrderRebalanceId(), JSONObject.toJSONString(orderRebalanceAuthConditionBO));
                return null;
            }

            //拷贝银行卡号，银行卡名称，银行卡logo
            BeanUtils.copyProperties(orderListBO, orderInfoListBO);
            //拷贝交易金额，交易费用
            if((orderRebalanceDO.getTransactionCharge() == null || orderRebalanceDO.getTransactionCharge().compareTo(BigDecimal.ZERO) == 0)
                    && orderInfoDO.getOrderStatus() == OrderStatusEnum.PROCESS.getCode()){
                //调仓申请完成后无手续费,发送查询订单请求
                SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderRebalanceDO.getAccountNumber(),
                        orderRebalanceDO.getMerchantNumber());
                log.info("{}调仓申请完成后查询订单详情,计算调仓费用：" + JSONObject.toJSONString(syncOrderRequest));

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
                    orderRebalanceDO.setTransactionCharge(transactionCharge);
                } else {
                    log.error("{}调仓申请完成后查询订单详情,计算调仓费用异常：" + JSONObject.toJSONString(syncOrderRequest));
                }
            }
            BeanUtils.copyProperties(orderRebalanceDO, orderInfoListBO);
            //设置下单时间
            orderInfoListBO.setSendTime(orderInfoDO.getSendTime());
            //设置交易类型
            orderInfoListBO.setBusinessCode(orderInfoDO.getBusinessCode());

            //设置交易时间
            orderInfoListBO.setTransactionDate(orderInfoDO.getSendTime());
            this.buildOrderRebalanceInfo(orderInfoListBO, orderInfoDO, orderRebalanceDO);

            //设置调仓比例
            orderInfoListBO.setRebalanceRate(this.getRebalanceRate(orderRebalanceDO));
            return orderInfoListBO;
        } catch (Exception e) {
            log.error("getOrderRebalanceDTO is error,orderInfoDO:{},orderListBO:{}", orderInfoDO.toString(), orderListBO.toString(), e);
        }
        return null;
    }

    private BigDecimal getRebalanceRate(OrderRebalanceDO orderRebalanceDO) {
        OrderRebalanceSummaryConditionBO querySummaryObj = new OrderRebalanceSummaryConditionBO();
        querySummaryObj.setOrderRebalaceId(orderRebalanceDO.getOrderRebalanceId());

        OrderRebalanceSummaryDO orderRebalanceSummaryDO = orderRebalanceSummaryMapper.queryByCondition(querySummaryObj);

        //调仓比例
        OrderRebalanceSummaryConditionBO querySummaryDeatilObj = new OrderRebalanceSummaryConditionBO();
        querySummaryDeatilObj.setOrderRebalanceSummaryId(orderRebalanceSummaryDO.getOrderRebalanceSummaryId());
        querySummaryDeatilObj.setTransactionType(OrderRebalanceTransactionTypeEnum.ORDERREBALANCE_INVEST.getCode());


        List<OrderRebalanceSummaryDetailDO> orderRebalanceSummaryDetailDOList =
                orderRebalanceSummaryDetailMapper.queryOrderRebalanceSummaryDetail(querySummaryDeatilObj);

        BigDecimal rebalanceRate = BigDecimal.ZERO;
        for (OrderRebalanceSummaryDetailDO orderRebalanceSummaryDetailDO : orderRebalanceSummaryDetailDOList) {
            rebalanceRate = rebalanceRate.add((orderRebalanceSummaryDetailDO.getPostProportion()
                    .subtract(orderRebalanceSummaryDetailDO.getPreProportion())).abs());
        }
        return rebalanceRate;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void syncOrderRebalance(List<OrderRebalanceDetailDO> addList, List<OrderRebalanceDetailDO> updateList, OrderRebalanceDO orderRebalanceDO) throws Exception {

        int total = 0;
        BigDecimal transactionCharge = BigDecimal.ZERO;
        int successNum = 0;
        int failureNum = 0;

        if (!CollectionUtils.isEmpty(addList)) {
            orderRebalanceDetailMapper.batchAddRebalanceDetailSync(addList);
            total = total + addList.size();
            // 交易费用
            for (OrderRebalanceDetailDO orderRebalanceDetailDO : addList) {
                // 成功
                if (TransactionStatusEnum.SUCCESS.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                    transactionCharge = transactionCharge.add(orderRebalanceDetailDO.getDiscountTransactionCharge());
                    successNum++;
                    // 失败
                } else if (TransactionStatusEnum.FAIL.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                    failureNum++;
                }
            }
        }

        if (!CollectionUtils.isEmpty(updateList)) {
            orderRebalanceDetailMapper.batchUpdateRebalanceDetail(updateList);
            total = total + updateList.size();
            for (OrderRebalanceDetailDO orderRebalanceDetailDO : updateList) {
                // 成功
                if (TransactionStatusEnum.SUCCESS.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                    transactionCharge = transactionCharge.add(orderRebalanceDetailDO.getDiscountTransactionCharge());
                    successNum++;
                    // 失败
                } else if (TransactionStatusEnum.FAIL.getCode() == orderRebalanceDetailDO.getTransactionStatus()) {
                    failureNum++;
                }
            }
        }

        Integer orderStatus = OrderStatusEnum.PROCESS.getCode();
        Integer transactionStatus = OrderStatusEnum.PROCESS.getCode();
        // 所有成功
        if (successNum == total) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.SUCCESS.getCode();
        } else if (failureNum == total) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.FAIL.getCode();
        } else if (successNum > 0 && failureNum > 0 && (successNum + failureNum == total)) {
            orderStatus = OrderStatusEnum.COMPLETE.getCode();
            transactionStatus = TransactionStatusEnum.SUCCESS_PART.getCode();
        }


        // 更新订单的信息-调仓
        this.updateOrderInfo4Rebalance(orderRebalanceDO.getUserId(), orderRebalanceDO.getOrderNo(), transactionCharge,
                orderRebalanceDO.getOrderRebalanceId(), transactionStatus, orderStatus);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RebalanceAuthResponse getRebalanceAutnBtn(String userId, Integer channel) throws Exception {
        RebalanceAuthResponse rebalanceResponse = new RebalanceAuthResponse();
        //调仓订单确认中,不可发起新调仓
        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
        orderRebalanceConditionBO.setUserId(userId);
        orderRebalanceConditionBO.setChannel(channel);
        orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.PROCESS.getCode());
        Long processRebalanceCount = orderInfoMapper.queryUnSendOrderRebalance(orderRebalanceConditionBO);
        if(processRebalanceCount > 0L){
            rebalanceResponse.setShowFlag(false);
            log.info("用户{}存在已发送奕丰但未完成的调仓订单,不显示调仓按钮!", userId);
            return rebalanceResponse;
        }

        //查询上一次调仓完成信息,间隔小于30日不可调仓
        orderRebalanceConditionBO.setTransactionStatus(OrderStatusEnum.COMPLETE.getCode());
        OrderInfoDO orderInfoDO = orderInfoMapper.queryLastRebalanceByCondition(orderRebalanceConditionBO);
        if(orderInfoDO != null){
            Date now = new Date();
            Date transactionFinishTime = orderInfoDO.getTransactionFinishTime();
            int daysBetweenDate = DateTimeUtil.getDaysBetweenDate(transactionFinishTime.getTime(),now.getTime());
            int rebalanceIntervalDays = environment.getProperty("rebalance.intervalDays", Integer.class, 30);
            if(daysBetweenDate <= rebalanceIntervalDays){
                rebalanceResponse.setShowFlag(false);
                log.info("用户{}上次调仓完成距今间隔小于{}天,不显示调仓按钮!", userId,rebalanceIntervalDays);
                return rebalanceResponse;
            }
        }

        OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
        orderRebalanceAuthConditionBO.setUserId(userId);
        orderRebalanceAuthConditionBO.setChannel(channel);
        orderRebalanceAuthConditionBO.setStartTime(DateTimeUtil.getStartOfDay(new Date()));
        orderRebalanceAuthConditionBO.setEndTime(DateTimeUtil.getEndOfDay(new Date()));
        orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        Long count = orderRebalanceAuthMapper.queryOrderRebalanceAuthCount(orderRebalanceAuthConditionBO);
        if (count <= 0L) {
            rebalanceResponse.setShowFlag(false);
        } else {
            rebalanceResponse.setShowFlag(true);
        }
        return rebalanceResponse;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RebalanceAuthResponse doRebalanceAutn(String userId, Integer channel) {
        RebalanceAuthResponse rebalanceResponse = new RebalanceAuthResponse();
        OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
        orderRebalanceAuthConditionBO.setUserId(userId);
        orderRebalanceAuthConditionBO.setChannel(channel);
        orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        OrderRebalanceAuthDO orderRebalanceAuthDO = orderRebalanceAuthMapper.queryOrderRebalanceAuth(orderRebalanceAuthConditionBO);

        OrderRebalanceAuthDO updateObj = new OrderRebalanceAuthDO();
        updateObj.setAuthStatus(OrderRebalanceAuthStatusEnum.COMPLETE.getCode());
        updateObj.setOrderRebalanceId(orderRebalanceAuthDO.getOrderRebalanceId());

        Long count = orderRebalanceAuthMapper.update(updateObj);
        if (count <= 0L) {
            rebalanceResponse.setDoAuthFlag(0L);
        } else {
            rebalanceResponse.setDoAuthFlag(1L);
        }
        return rebalanceResponse;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateOrderSendTime(OrderRebalanceSendBO orderRebalanceSendBO) {
        Long orderNo = orderRebalanceSendBO.getOrderNo();
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setOrderNo(orderNo);
        orderInfoDO.setSendTime(new Date());
        int k = orderInfoMapper.update(orderInfoDO);
        if (k <= 0) {
            log.error("调仓任务:更新订单发送时间：OrderNo={}", orderNo);
            throw new RuntimeException("调仓任务:更新订单发送时间:OrderNo=" + orderNo);
        }
    }


    /**
     * @param orderInfoListBO
     * @param orderInfoDO
     * @param orderRebalanceDO
     * @return
     * @Description (用一句话描述这个变量表示什么)
     * @author lizengqiang
     * @date 2018/4/24 10:22
     */
    private OrderInfoListBO buildOrderRebalanceInfo(OrderInfoListBO orderInfoListBO, OrderInfoDO orderInfoDO, OrderRebalanceDO orderRebalanceDO) {
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
        OrderRebalanceDetailRequest orderRebalanceDetailRequest = new OrderRebalanceDetailRequest();
        orderRebalanceDetailRequest.setOrderRebalanceId(orderRebalanceDO.getOrderRebalanceId());
        List<OrderRebalanceDetailBO> orderRebalanceDetailList = orderRebalanceDetailMapper.listByOrderRebalanceDetailCondition(orderRebalanceDetailRequest);
        if (CollectionUtils.isEmpty(orderRebalanceDetailList)) {
            return orderInfoListBO;
        }
        //发起调仓失败
        /*if(orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.APPLY_FAIL.getCode()){
            //取sendTime,手续费为0
            orderInfoListBO.setPricedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setCompletedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_FAIL.getCode());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            return orderInfoListBO;
        }*/

        //下单成功后,设置最早一支赎回预计确认日期,最后一支申购预计确认日期
        List<OrderRebalanceDetailDO> applySuccessOrderRebalanceDetail = this.buildApplySuccessOrderRebalanceDetail(orderRebalanceDetailList);
        if(CollectionUtils.isNotEmpty(applySuccessOrderRebalanceDetail)){
            //调仓需要赎回的
            List<OrderRebalanceDetailDO> rebalanceNeedRedeemDetail = Lists.newArrayList();
            //调仓需要申购的
            List<OrderRebalanceDetailDO> rebalanceNeedInvestDetail = Lists.newArrayList();
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (OrderRebalanceDetailDO orderRebanceDetail:applySuccessOrderRebalanceDetail) {
                transactionCharge = transactionCharge.add(orderRebanceDetail.getDiscountTransactionCharge());
                if(orderRebanceDetail.getTransactionType() == 1){//赎回
                    rebalanceNeedRedeemDetail.add(orderRebanceDetail);
                }else if(orderRebanceDetail.getTransactionType() == 2){//申购
                    rebalanceNeedInvestDetail.add(orderRebanceDetail);
                }
            }
            if(CollectionUtils.isNotEmpty(rebalanceNeedRedeemDetail)){
                rebalanceNeedRedeemDetail.sort((o1,o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
                orderInfoListBO.setPricedDate(rebalanceNeedRedeemDetail.get(0).getExpectedConfirmedDate());
            }
            if(CollectionUtils.isNotEmpty(rebalanceNeedInvestDetail)){
                rebalanceNeedInvestDetail.sort((o1,o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
                orderInfoListBO.setCompletedDate(rebalanceNeedInvestDetail.get(rebalanceNeedInvestDetail.size() - 1).getExpectedConfirmedDate());
            }
            orderInfoListBO.setTransactionCharge(transactionCharge);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_SUCCESS.getCode());
        }

        //设置调仓处理的订单状态，交易时间，预计调仓完成时间
        List<OrderRebalanceDetailDO> pricedOrderRebalanceDetail = this.buildPricedOrderRebalanceDetail(orderRebalanceDetailList);
        if (CollectionUtils.isNotEmpty(pricedOrderRebalanceDetail)) {
            pricedOrderRebalanceDetail.sort((OrderRebalanceDetailDO o1, OrderRebalanceDetailDO o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
            //设置交易日期，设置预计确认时间，设置订单状态
//            orderInfoListBO.setTransactionDate(pricedOrderRebalanceDetail.get(0).getPricedDate());
//            orderInfoListBO.setExpectPricedDate(pricedOrderRebalanceDetail.get(pricedOrderRebalanceDetail.size() - 1).getPricedDate());
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICE.getCode());

            //调仓需要赎回的
            List<OrderRebalanceDetailDO> rebalanceNeedRedeemDetail = Lists.newArrayList();
            //调仓需要申购的
            List<OrderRebalanceDetailDO> rebalanceNeedInvestDetail = Lists.newArrayList();
            BigDecimal transactionCharge = BigDecimal.ZERO;
            //设置最早一支赎回确认日期
            for (OrderRebalanceDetailDO orderRebanceDetail:pricedOrderRebalanceDetail) {
                transactionCharge = transactionCharge.add(orderRebanceDetail.getDiscountTransactionCharge());
                if(orderRebanceDetail.getTransactionType() == 1){//赎回
                    rebalanceNeedRedeemDetail.add(orderRebanceDetail);
                }else if(orderRebanceDetail.getTransactionType() == 2){//申购
                    rebalanceNeedInvestDetail.add(orderRebanceDetail);
                }
            }
            if(CollectionUtils.isNotEmpty(rebalanceNeedRedeemDetail)){
                rebalanceNeedRedeemDetail.sort((o1,o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
                orderInfoListBO.setPricedDate(rebalanceNeedRedeemDetail.get(0).getPricedDate());
            }

            if(CollectionUtils.isNotEmpty(rebalanceNeedInvestDetail)){
                rebalanceNeedInvestDetail.sort((o1,o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
                orderInfoListBO.setCompletedDate(rebalanceNeedInvestDetail.get(rebalanceNeedInvestDetail.size() - 1).getExpectedConfirmedDate());
            }
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICED.getCode());

            orderInfoListBO.setTransactionCharge(transactionCharge);
        }
        //设置调仓完成的时间，设置订单状态
        List<OrderRebalanceDetailDO> buildCompletedOrderRebalance = this.buildCompletedOrderRebalanceDetail(orderRebalanceDetailList);
        if (CollectionUtils.isNotEmpty(buildCompletedOrderRebalance) && buildCompletedOrderRebalance.size() == orderRebalanceDetailList.size()) {
            buildCompletedOrderRebalance.sort((OrderRebalanceDetailDO o1, OrderRebalanceDetailDO o2) -> o1.getCompletedDate().compareTo(o2.getCompletedDate()));
            //设置完成时间，设置订单状态
            orderInfoListBO.setCompletedDate(buildCompletedOrderRebalance.get(buildCompletedOrderRebalance.size() - 1).getCompletedDate());
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMPLETE.getCode());
            BigDecimal transactionCharge = BigDecimal.ZERO;
            //调仓需要赎回的
            List<OrderRebalanceDetailDO> rebalanceNeedRedeemDetail = Lists.newArrayList();
            //调仓需要申购的
            List<OrderRebalanceDetailDO> rebalanceNeedInvestDetail = Lists.newArrayList();
            //设置最早一支赎回确认日期
            for (OrderRebalanceDetailDO orderRebanceDetail:buildCompletedOrderRebalance) {
                transactionCharge = transactionCharge.add(orderRebanceDetail.getDiscountTransactionCharge());
                if(orderRebanceDetail.getTransactionType() == 1){//赎回
                    rebalanceNeedRedeemDetail.add(orderRebanceDetail);
                }else if(orderRebanceDetail.getTransactionType() == 2){//申购
                    rebalanceNeedInvestDetail.add(orderRebanceDetail);
                }
            }
            if(CollectionUtils.isNotEmpty(rebalanceNeedRedeemDetail)){
                rebalanceNeedRedeemDetail.sort((o1,o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
                orderInfoListBO.setPricedDate(rebalanceNeedRedeemDetail.get(0).getPricedDate());
            }

            if(CollectionUtils.isNotEmpty(rebalanceNeedInvestDetail)){
                rebalanceNeedInvestDetail.sort((o1,o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
                orderInfoListBO.setCompletedDate(rebalanceNeedInvestDetail.get(rebalanceNeedInvestDetail.size() - 1).getPricedDate());
            }

            orderInfoListBO.setTransactionCharge(transactionCharge);
        }
        //如果详情有一个失败就调仓失败，手续费为0
        List<OrderRebalanceDetailDO> buildFailedOrderRebalance = this.buildFailedOrderRebalanceDetail(orderRebalanceDetailList);
        if (CollectionUtils.isNotEmpty(buildFailedOrderRebalance)) {
            buildFailedOrderRebalance.sort(( o1, o2) -> o1.getVoidDate().compareTo(o2.getVoidDate()));
            orderInfoListBO.setPricedDate(buildFailedOrderRebalance.get(buildFailedOrderRebalance.size() - 1).getVoidDate());
            orderInfoListBO.setCompletedDate(buildFailedOrderRebalance.get(buildFailedOrderRebalance.size() - 1).getVoidDate());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
        }

        //如果详情有一个失败就失败，手续费为0
//        if(!this.orderRebalanceDetailAllFail(buildCompletedOrderRebalance)){
//            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
//        }
        return orderInfoListBO;
    }

    private List<OrderRebalanceDetailDO> buildFailedOrderRebalanceDetail(List<OrderRebalanceDetailBO> orderRebalanceDetailList) {
        List<OrderRebalanceDetailDO> result = Lists.newArrayList();
        for (OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList) {
            Integer transactionStatus = orderRebalanceDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //交易失败
            if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date voidDate = orderRebalanceDetailDO.getVoidDate();
                if (voidDate != null) {
                    result.add(orderRebalanceDetailDO);
                }
            }
        }
        return result;
    }

    private List<OrderRebalanceDetailDO> buildApplySuccessOrderRebalanceDetail(List<OrderRebalanceDetailBO> orderRebalanceDetailList) {
        List<OrderRebalanceDetailDO> result = Lists.newArrayList();
        for (OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList) {
            Integer transactionStatus = orderRebalanceDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //下单成功,确认中,确认成功,等待付款
            if (transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.WAIT_PAY.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.RECEIVING.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()) {
                Date expectedConfirmedDate = orderRebalanceDetailDO.getExpectedConfirmedDate();
                if (expectedConfirmedDate != null) {
                    result.add(orderRebalanceDetailDO);
                }
            } else {
                return null;
            }
        }
        return result;
    }

    private Boolean orderRebalanceDetailAllFail(List<OrderRebalanceDetailDO> orderRebalanceDetailList){
        if(CollectionUtils.isEmpty(orderRebalanceDetailList)){
            return false;
        }
        Boolean flag=true;
        for(OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList){
            Integer transactionStatus=orderRebalanceDetailDO.getTransactionStatus();
//            if(transactionStatus==null||transactionStatus.intValue() != TransactionStatusEnum.FAIL.getCode()){
            if(transactionStatus!=null && transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()){
                flag=false;
                break;
            }
        }
        return flag;
    }

    private List<OrderRebalanceDetailDO> buildPricedOrderRebalanceDetail(List<OrderRebalanceDetailBO> orderRebalanceDetailList) {
        List<OrderRebalanceDetailDO> result = Lists.newArrayList();
        for (OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList) {
            Integer transactionStatus = orderRebalanceDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                //continue;
                return null;
            }
            //transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode() ||
            if (transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
//                    || transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date pricedDate = orderRebalanceDetailDO.getPricedDate();
                if (pricedDate != null) {
                    result.add(orderRebalanceDetailDO);
                }
            }else {
                return null;
            }
        }
        return result;
    }

    private List<OrderRebalanceDetailDO> buildCompletedOrderRebalanceDetail(List<OrderRebalanceDetailBO> orderRebalanceDetailList) {
        List<OrderRebalanceDetailDO> result = Lists.newArrayList();
        for (OrderRebalanceDetailDO orderRebalanceDetailDO : orderRebalanceDetailList) {
            Integer transactionStatus = orderRebalanceDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //  || transactionStatus.intValue() == TransactionStatusEnum.CANCELED.getCode()
            if (transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date completedDate = orderRebalanceDetailDO.getCompletedDate();
                if (completedDate != null) {
                    result.add(orderRebalanceDetailDO);
                }
            }/* else if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date completedDate = orderRebalanceDetailDO.getVoidDate();
                if (completedDate != null) {
                    orderRebalanceDetailDO.setCompletedDate(completedDate);
                    result.add(orderRebalanceDetailDO);
                }
            } else if (transactionStatus.intValue() == TransactionStatusEnum.PAY_FAIL.getCode()) {
                Date completedDate = orderRebalanceDetailDO.getCreateTime();
                if (completedDate != null) {
                    orderRebalanceDetailDO.setCompletedDate(completedDate);
                    result.add(orderRebalanceDetailDO);
                }
            }else {
                return null;
            }*/
        }
        return result;
    }

    @Override
    public OrderRebalanceSummaryDO getLastRebalanceSummaryByCondition(OrderRebalanceSummaryConditionBO orderRebalanceSummaryConditionBO) {
        return orderRebalanceSummaryMapper.getLastRebalanceSummaryByCondition(orderRebalanceSummaryConditionBO);
    }

    @Override
    public List<OrderRebalanceSummaryDetailDO> getRebalanceSummaryDetailByCondition(OrderRebalanceSummaryConditionBO orderRebalanceSummaryConditionBO) {
        return orderRebalanceSummaryDetailMapper.queryOrderRebalanceSummaryDetail(orderRebalanceSummaryConditionBO);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RebalanceConfirmResponse confirmRebalance(RebalanceRequest requeset) {

        RebalanceConfirmResponse response = new RebalanceConfirmResponse();
        //1.更改授权状态
        OrderRebalanceAuthDO orderRebalanceAuthDO = null;
//        RebalanceAuthResponse rebalanceResponse = new RebalanceAuthResponse();
        OrderRebalanceAuthConditionBO orderRebalanceAuthConditionBO = new OrderRebalanceAuthConditionBO();
        orderRebalanceAuthConditionBO.setUserId(requeset.getUserId());
        orderRebalanceAuthConditionBO.setChannel(requeset.getChannel());
        orderRebalanceAuthConditionBO.setAuthStatus(OrderRebalanceAuthStatusEnum.PROCESS.getCode());
        orderRebalanceAuthDO = orderRebalanceAuthMapper.queryOrderRebalanceAuth(orderRebalanceAuthConditionBO);

        if(orderRebalanceAuthDO == null){
            log.error("查询用户调仓未授权信息未空：userId={}",requeset.getUserId());
            throw new RuntimeException("查询用户调仓未授权信息未空,userId="+requeset.getUserId());
        }

        OrderRebalanceAuthDO updateObj = new OrderRebalanceAuthDO();
        updateObj.setAuthStatus(OrderRebalanceAuthStatusEnum.COMPLETE.getCode());
        updateObj.setOrderRebalanceId(orderRebalanceAuthDO.getOrderRebalanceId());

        Long count = orderRebalanceAuthMapper.update(updateObj);
        if (count <= 0L) {
            log.error("更新用户调仓未授权信息为授权失败：userId={},orderRebalanceId={}",requeset.getUserId(),orderRebalanceAuthDO.getOrderRebalanceId());
            throw new RuntimeException("更新用户调仓未授权信息为授权失败,userId="+requeset.getUserId());
        }

        //2.发送给奕丰
        OrderRebalanceConditionBO orderRebalanceConditionBO = new OrderRebalanceConditionBO();
        orderRebalanceConditionBO.setOrderRebalanceId(orderRebalanceAuthDO.getOrderRebalanceId());
        OrderRebalanceDO orderRebalanceDO = orderRebalanceMapper.queryOrderRebalanceDOByCondition(orderRebalanceConditionBO);

        if(orderRebalanceDO == null){
            log.error("查询用户调仓信息未空：userId={},orderRebalanceId={}",requeset.getUserId(),orderRebalanceAuthDO.getOrderRebalanceId());
            throw new RuntimeException("查询用户调仓信息为空,userId="+requeset.getUserId());
        }

        OrderRebalanceSendBO orderRebalanceSendBO = new OrderRebalanceSendBO();
        BeanUtils.copyProperties(orderRebalanceDO,orderRebalanceSendBO);
        orderRebalanceSendBO.setThirdAccountNumber(orderRebalanceDO.getAccountNumber());
        try {
            BaseResponse<YfRebalancePortfolioResponse> baseResponse = fundRebalanceBiz.confirmRebalance(orderRebalanceSendBO);
            if (baseResponse.success()) {
                YfRebalancePortfolioResponse rebalancePortfolioResponse = baseResponse.getData();
                // 返回的调仓明细列表判断
                if (CollectionUtils.isEmpty(rebalancePortfolioResponse.getRebalanceResult())) {
                    log.error("{}发送调仓请求到奕丰处理,调仓明细列表空异常" + JSONObject.toJSONString(baseResponse), Thread.currentThread());
                    throw new RuntimeException("发送调仓请求到奕丰处理,调仓明细列表空异常" + JSONObject.toJSONString(baseResponse));
                }
                // 调仓交易发送后处理
                rebalanceService.rebalanceAfterDeal(rebalancePortfolioResponse, orderRebalanceSendBO);

                //组织数据
                OrderRebalanceSummaryConditionBO orscBO = new OrderRebalanceSummaryConditionBO();
                orscBO.setOrderRebalaceId(orderRebalanceDO.getOrderRebalanceId());
                OrderRebalanceSummaryDO orderRebalanceSummaryDO = orderRebalanceSummaryMapper.queryByCondition(orscBO);
                response.setStatus(1);
                response.setRebalanceCharge(orderRebalanceSummaryDO.getTotalFee().setScale(2, RoundingMode.HALF_DOWN));
                BigDecimal investAmount = BigDecimal.ZERO;

                List<YfRebalanceDetail> rebalanceResult = rebalancePortfolioResponse.getRebalanceResult();
                List<YfRebalanceDetail> redeemResult = new ArrayList<>();
                List<YfRebalanceDetail> investResult = new ArrayList<>();
                for(YfRebalanceDetail yfRebalanceDetail : rebalanceResult){
                    if("sell".equals(yfRebalanceDetail.getTransactionType())){
                        redeemResult.add(yfRebalanceDetail);
                    }else if("buy".equals(yfRebalanceDetail.getTransactionType())){
                        investResult.add(yfRebalanceDetail);
                        investAmount = investAmount.add(yfRebalanceDetail.getInvestmentAmount());
                    }
                }
                response.setRebalanceAmount(investAmount.setScale(2, RoundingMode.HALF_DOWN));

                DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
                Calendar calendar = Calendar.getInstance();
                if (CollectionUtils.isNotEmpty(redeemResult)) {
                    redeemResult.sort((o1, o2) -> Integer.parseInt(o1.getTransactionSettleLagDay()) - (Integer.parseInt(o2.getTransactionSettleLagDay())));
                    calendar.setTimeInMillis(Long.parseLong(redeemResult.get(0).getTransactionDate()));
                    response.setRedeemDateStr(getDateAndWeekStr(formatter.format(calendar.getTime()),Integer.parseInt(redeemResult.get(0).getTransactionSettleLagDay())));
                }
                if (CollectionUtils.isNotEmpty(investResult)) {
                    investResult.sort((o1, o2) -> Integer.parseInt(o1.getTransactionCfmLagDay()) - (Integer.parseInt(o2.getTransactionCfmLagDay())));
                    calendar.setTimeInMillis(Long.parseLong(investResult.get(0).getTransactionDate()));
                    response.setInvestDateStr(getDateAndWeekStr(formatter.format(calendar.getTime()),Integer.parseInt(investResult.get(0).getTransactionCfmLagDay())));
                    calendar.setTimeInMillis(Long.parseLong(investResult.get(investResult.size() - 1).getTransactionDate()));
                    response.setCompleteDateStr(getDateAndWeekStr(formatter.format(calendar.getTime()),Integer.parseInt(investResult.get(investResult.size() - 1).getTransactionCfmLagDay())));
                }
            } else {
                String respCode = baseResponse.getCode();
                if ("108".equals(respCode) || "207".equals(respCode) || "208".equals(respCode)
                        || "209".equals(respCode) || "414".equals(respCode) || "137".equals(respCode)) {
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
                response.setStatus(0);
            }
        } catch (Throwable e) {
            log.error("{}发送调仓请求到奕丰处理,异常" + JSONObject.toJSONString(orderRebalanceSendBO), Thread.currentThread(),e);
            throw new RuntimeException("{}发送调仓请求到奕丰处理,异常" + JSONObject.toJSONString(orderRebalanceSendBO));
        }

        return response;
    }

    private String getWeek(String sdate) {
        // 再转换为时间
        Date date = strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        // int hour=c.get(Calendar.DAY_OF_WEEK);
        // hour中存的就是星期几了，其范围 1~7
        // 1=星期日 7=星期六，其他类推
        return new SimpleDateFormat("EEEE", Locale.CHINA).format(c.getTime());
    }

    private Date strToDate(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(strDate, pos);
        return strtodate;
    }

    private String getDateAndWeekStr(String dateStr,int days){
        Date date = strToDate(dateStr);
        for(int i = 0; i < days;i++){
            date = DateUtil.addDays(date, 1);
            DateTime dateTime = new DateTime(date.getTime());
            if (DateTimeConstants.SATURDAY == dateTime.getDayOfWeek()) {
                date = dateTime.plusDays(2).toDate();
            }
        }
        return new SimpleDateFormat("yyyy/MM/dd(EEEE)", Locale.CHINA).format(date.getTime());
    }

    @Override
    public List<OrderRebalanceAsyncBO> queryOrderRebalanceRepaired(Integer channel, Integer businessCode,
                                                                Integer pagetIndex, Integer pageSize) {
        List<OrderRebalanceAsyncBO> orderRebalanceAsyncDtoList =
                orderRebalanceMapper.queryOrderRebalanceRepaired(channel, businessCode,  pagetIndex, pageSize);
        return orderRebalanceAsyncDtoList;
    }

}
