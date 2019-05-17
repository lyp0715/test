package com.snb.deal.service.impl.order;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.datetime.DateUtil;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.*;
import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderInvestListRequest;
import com.snb.deal.api.dto.plan.PlanAutoInvestRequest;
import com.snb.deal.api.enums.order.InvestTypeEnum;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderInvestAdminBO;
import com.snb.deal.bo.order.OrderInvestBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.enums.*;
import com.snb.deal.mapper.order.OrderInfoMapper;
import com.snb.deal.mapper.order.OrderInvestDetailMapper;
import com.snb.deal.mapper.order.OrderInvestMapper;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.SyncOrderRequest;
import com.snb.third.yifeng.dto.order.SyncOrderResponse;
import com.snb.third.yifeng.dto.order.invest.YfFundInvestResponse;
import com.snb.third.yifeng.dto.order.invest.YfFundInvestResponseDetail;
import com.snb.third.yifeng.dto.order.invest.YfInvestResponse;
import com.snb.third.yifeng.dto.order.invest.YfInvestResponseDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OrderInvestServiceImpl implements OrderInvestService {
    @Autowired
    private OrderInvestMapper orderInvestMapper;
    @Autowired
    private OrderInvestDetailMapper orderInvestDetailMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private GuidCreater guidCreater;
    @Resource
    private FlowNumberService flowNumberService;
    @Resource
    private AmqpTemplate amqpTemplate;
    @Resource
    FundPortfolioService fundPortfolioService;

    @Override
    public PageInfo<OrderInvestAdminBO> pageOrderInvest(OrderInvestListRequest request) {
        PageHelper.startPage(request.getPage(), request.getPageSize(), true);
        List<OrderInvestAdminBO> list = orderInvestMapper.listByOrderInvestListCondition(request);
        return new PageInfo<>(list);
    }

    @Override
    public List<OrderInvestDetailDO> listOrderInvestDetail(OrderInvestDetailRequest condition) {
        return orderInvestDetailMapper.listByOrderInvestDetailCondition(condition);
    }

    @Override
    public PageInfo<OrderInvestDO> pageOrderPlanAutoInvest(PlanAutoInvestRequest request) {
        PageHelper.startPage(request.getPageNo(), request.getPageSize(), true);
        List<OrderInvestDO> list = orderInvestMapper.listByAutoInvestCondition(request);
        return new PageInfo<>(list);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public OrderInvestDO createInvestOrder(OrderInvestDO orderInvestDO) throws Exception {
        //初始化order_info
        //初始化order_invest
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setUserId(orderInvestDO.getUserId());
        orderInfoDO.setOrderNo(guidCreater.getUniqueID());
        orderInfoDO.setOrderStatus(OrderStatusEnum.PROCESS.getCode());
        orderInfoDO.setBusinessCode(OrderBusinessEnum.MANUL_INVEST.getCode());
        orderInfoDO.setSource("");
        orderInfoDO.setTransactionAmount(orderInvestDO.getTransactionAmount());
        orderInfoDO.setSendTime(new Date());
        orderInfoDO.setChannel(orderInvestDO.getChannel());

        orderInfoMapper.insert(orderInfoDO);

        //实际交易金额为0
        orderInvestDO.setTransactionAmount(orderInvestDO.getTransactionAmount());
        orderInvestDO.setOrderInvestId(guidCreater.getUniqueID());
        orderInvestDO.setOrderNo(orderInfoDO.getOrderNo());
        orderInvestDO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
        orderInvestDO.setContractNumber("");
        orderInvestDO.setTransactionStatus(0);//TODO
        orderInvestDO.setTransactionCharge(BigDecimal.ZERO);
        orderInvestDO.setResponseCode("");
        orderInvestDO.setResponseMessage("");

        orderInvestMapper.insert(orderInvestDO);

        return orderInvestDO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public OrderInvestDO afterInvestFailed(Long orderInvestId, Long orderNo, String code, String message) throws Exception {

        //更新订单为失败
        orderInfoMapper.updateOrderStatus(orderNo, OrderStatusEnum.APPLY_FAIL.getCode());

        //更新投资订单为失败
        //OrderInvestResponse orderInvestResponse = new OrderInvestResponse();
        OrderInvestDO orderInvestDOParam = new OrderInvestDO();

        orderInvestDOParam.setOrderInvestId(orderInvestId);
        orderInvestDOParam.setOrderNo(orderNo);
        orderInvestDOParam.setResponseCode(code);
        orderInvestDOParam.setResponseMessage(message);
        orderInvestDOParam.setTransactionStatus(0);//TODO 订单状态
        orderInvestMapper.updateOrderInvest(orderInvestDOParam);

//        orderInvestDO.setResponseCode(baseResponse.getCode());
//        orderInvestDO.setResponseMessage(baseResponse.getMessage());
        //TODO
        return new OrderInvestDO();
    }

    @Event(reliability = true,
            eventType = "'investApply'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.invest.apply",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void afterInvestApply(OrderInvestDO orderInvestDO, BaseResponse<YfInvestResponse> baseResponse) throws Exception {

        if (!Objects.isNull(baseResponse) && !Objects.isNull(baseResponse.getData())) {
            List<OrderInvestDetailDO> orderInvestDetailDOS = this.buildInvestDetail(baseResponse.getData(), orderInvestDO);
            orderInvestDetailMapper.insertBatch(orderInvestDetailDOS);
            //更新订单交易金额和手续费
            OrderInvestDO param = new OrderInvestDO();
            param.setOrderInvestId(orderInvestDO.getOrderInvestId());
            param.setTransactionAmount(orderInvestDO.getTransactionAmount());
            param.setTransactionCharge(orderInvestDO.getTransactionCharge());
            orderInvestMapper.updateOrderInvest(param);
        }

        InvestOrderMessage message = new InvestOrderMessage();
        message.setUserId(orderInvestDO.getUserId());
        message.setAccountNumber(orderInvestDO.getAccountNumber());
        message.setOrderNo(orderInvestDO.getOrderNo());
        message.setMerchantNumber(orderInvestDO.getMerchantNumber());
        message.setEventId(orderInvestDO.getUserId() + orderInvestDO.getOrderNo());

        EventMessageContext.addMessage(message);
    }

    @Override
    public List<OrderInfoListBO> getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO) {
        List<OrderInfoListBO> result = Lists.newArrayList();
        OrderInvestDO orderInvestParam = new OrderInvestDO();
        orderInvestParam.setOrderNo(orderInfoDO.getOrderNo());
        List<OrderInvestDO> orderInvestList = orderInvestMapper.queryByOrderNo(orderInvestParam);
        if (CollectionUtils.isEmpty(orderInvestList)) {
            return result;
        }
        for (OrderInvestDO orderInvestDO : orderInvestList) {
            try {
                OrderInfoListBO orderInfoListBO = new OrderInfoListBO();
                //拷贝银行卡号，银行卡名称，银行卡logo
                BeanUtils.copyProperties(orderListBO, orderInfoListBO);
                //拷贝交易金额，交易费用，
                if((orderInvestDO.getTransactionCharge() == null || orderInvestDO.getTransactionCharge().compareTo(BigDecimal.ZERO) == 0)
                        && orderInfoDO.getOrderStatus() == OrderStatusEnum.PROCESS.getCode()){
                    //买入申请完成后无手续费,发送查询订单请求
                    SyncOrderRequest syncOrderRequest = new SyncOrderRequest(orderInvestDO.getAccountNumber(),
                            orderInvestDO.getMerchantNumber());
                    log.info("{}买入申请完成后查询订单详情,计算买入费用：" + JSONObject.toJSONString(syncOrderRequest));

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
                        orderInvestDO.setTransactionCharge(transactionCharge);
                    } else {
                        log.error("{}买入申请完成后查询订单详情,计算买入费用异常：" + JSONObject.toJSONString(syncOrderRequest));
                    }
                }
                BeanUtils.copyProperties(orderInvestDO, orderInfoListBO);
                //设置下单时间
                orderInfoListBO.setSendTime(orderInfoDO.getSendTime());
                //设置交易类型
                orderInfoListBO.setBusinessCode(orderInfoDO.getBusinessCode());
                //设置交易时间
                orderInfoListBO.setTransactionDate(orderInfoDO.getSendTime());
                this.buildOrderInvestInfo(orderInfoListBO, orderInfoDO, orderInvestDO);
                result.add(orderInfoListBO);
            } catch (Exception e) {
                log.error("getOrderInvestDTO is error,orderInfoDO:{},orderListBO:{}", orderInfoDO.toString(), orderListBO.toString(), e);
            }
        }
        return result;
    }

    private OrderInfoListBO buildOrderInvestInfo(OrderInfoListBO orderInfoListBO, OrderInfoDO orderInfoDO, OrderInvestDO orderInvestDO) {
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
        OrderInvestDetailRequest orderInvestDetailRequest = new OrderInvestDetailRequest();
        orderInvestDetailRequest.setOrderInvestId(orderInvestDO.getOrderInvestId());
        List<OrderInvestDetailDO> orderInvestDetailDOList = orderInvestDetailMapper.listByOrderInvestDetailCondition(orderInvestDetailRequest);
        if (CollectionUtils.isEmpty(orderInvestDetailDOList)) {
            return orderInfoListBO;
        }

        //发起买入失败(支付失败):全失败才算失败
       List<OrderInvestDetailDO> payFailDetails = this.buildPayFailOrderInvestDetail(orderInvestDetailDOList);
        if (CollectionUtils.isNotEmpty(payFailDetails) && payFailDetails.size() == orderInvestDetailDOList.size()) {
//            payFailDetails.sort((OrderInvestDetailDO o1, OrderInvestDetailDO o2) -> o1.getCompletedDate().compareTo(o2.getCompletedDate()));
//            //设置完成时间，设置订单状态
//            orderInfoListBO.setCompletedDate(payFailDetails.get(payFailDetails.size() - 1).getCompletedDate());
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_FAIL.getCode());

            //支付失败,取sendTime,手续费为0
            orderInfoListBO.setPricedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setCompletedDate(orderInfoDO.getSendTime());
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_FAIL.getCode());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            return orderInfoListBO;
        }

        //下单成功后,设置预期确认时间和预期查看收益时间,买入金额和预计手续费
        List<OrderInvestDetailDO> applySuccessOrderInvestDetail = this.bulidApplySuccessOrderInvestDetail(orderInvestDetailDOList);
        if(CollectionUtils.isNotEmpty(applySuccessOrderInvestDetail)){
            applySuccessOrderInvestDetail.sort((o1,o2) -> o1.getExpectedConfirmedDate().compareTo(o2.getExpectedConfirmedDate()));
            orderInfoListBO.setPricedDate(applySuccessOrderInvestDetail.get(applySuccessOrderInvestDetail.size() - 1).getExpectedConfirmedDate());
            Calendar cal = Calendar.getInstance();
            cal.setTime(applySuccessOrderInvestDetail.get(0).getExpectedConfirmedDate());
            int offset = 1;
            if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY){
                offset = 3;
            }else if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY){
                offset = 2;
            }
            orderInfoListBO.setCompletedDate(DateUtil.addDays(applySuccessOrderInvestDetail.get(0).getExpectedConfirmedDate(),offset));
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMMIT_APPLY_SUCCESS.getCode());
            BigDecimal transactionCharge = BigDecimal.ZERO;
            BigDecimal actualTransactionAmount = BigDecimal.ZERO;
            for (OrderInvestDetailDO orderInvestDetailDO : applySuccessOrderInvestDetail){
                actualTransactionAmount = actualTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                transactionCharge = transactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
            }
            orderInfoListBO.setActualTransactionAmount(actualTransactionAmount);
            orderInfoListBO.setTransactionCharge(transactionCharge);
        }

        int failCount = 0;
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList){
           if(orderInvestDetailDO.getTransactionStatus().intValue() == TransactionStatusEnum.PAY_FAIL.getCode()
                   || orderInvestDetailDO.getTransactionStatus().intValue() == TransactionStatusEnum.FAIL.getCode()){
               failCount++;
           }
        }
        //设置确认份额的订单状态
        List<OrderInvestDetailDO> pricedOrderInvestDetail = this.buildPricedOrderInvestDetail(orderInvestDetailDOList);
        if (CollectionUtils.isNotEmpty(pricedOrderInvestDetail) && orderInvestDetailDOList.size() - failCount == pricedOrderInvestDetail.size()) {
            pricedOrderInvestDetail.sort((OrderInvestDetailDO o1, OrderInvestDetailDO o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
//            orderInfoListBO.setTransactionDate(pricedOrderRedeemDetail.get(0).getPricedDate());
//            orderInfoListBO.setExpectPricedDate(pricedOrderRedeemDetail.get(pricedOrderRedeemDetail.size() - 1).getPricedDate());
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICE.getCode());
            //最后一支确认日期
            orderInfoListBO.setPricedDate(pricedOrderInvestDetail.get(pricedOrderInvestDetail.size() - 1).getPricedDate());
            Calendar cal = Calendar.getInstance();
            cal.setTime(pricedOrderInvestDetail.get(0).getPricedDate());
            int offset = 1;
            if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY){
                offset = 3;
            }else if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY){
                offset = 2;
            }
            //最前一支确认日期+1
            orderInfoListBO.setCompletedDate(DateUtil.addDays(pricedOrderInvestDetail.get(0).getPricedDate(),offset));
            BigDecimal actualTransactionAmount = BigDecimal.ZERO;
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (OrderInvestDetailDO orderInvestDetailDO : pricedOrderInvestDetail){
                actualTransactionAmount = actualTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                transactionCharge = transactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
            }
            orderInfoListBO.setActualTransactionAmount(actualTransactionAmount);
            orderInfoListBO.setTransactionCharge(transactionCharge);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.PRICED.getCode());
        }
        //设置完成中的完成时间，设置订单状态
        List<OrderInvestDetailDO> buildCompletedOrderInvest = this.buildCompletedOrderInvestDetail(orderInvestDetailDOList);
        if (CollectionUtils.isNotEmpty(buildCompletedOrderInvest) && buildCompletedOrderInvest.size() == orderInvestDetailDOList.size() - failCount) {
            buildCompletedOrderInvest.sort((OrderInvestDetailDO o1, OrderInvestDetailDO o2) -> o1.getCompletedDate().compareTo(o2.getCompletedDate()));
            //设置完成时间，设置订单状态
//            orderInfoListBO.setCompletedDate(buildCompletedOrderInvest.get(buildCompletedOrderInvest.size() - 1).getCompletedDate());
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMPLETE.getCode());

            //最前一支确认日期+1(暂时跳过周末)
            pricedOrderInvestDetail.sort((o1, o2) -> o1.getPricedDate().compareTo(o2.getPricedDate()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(pricedOrderInvestDetail.get(0).getPricedDate());
            int offset = 1;
            if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY){
                offset = 3;
            }else if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY){
                offset = 2;
            }
            orderInfoListBO.setPricedDate(pricedOrderInvestDetail.get(pricedOrderInvestDetail.size() - 1).getPricedDate());
            orderInfoListBO.setCompletedDate(DateUtil.addDays(pricedOrderInvestDetail.get(0).getPricedDate(),offset));
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.COMPLETE.getCode());
            BigDecimal actualTransactionAmount = BigDecimal.ZERO;
            BigDecimal transactionCharge = BigDecimal.ZERO;
            for (OrderInvestDetailDO orderInvestDetailDO : buildCompletedOrderInvest){
                actualTransactionAmount = actualTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                transactionCharge = transactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
            }
            orderInfoListBO.setActualTransactionAmount(actualTransactionAmount);
            orderInfoListBO.setTransactionCharge(transactionCharge);
        }

        //设置买入失败的时间，设置订单状态,手续费为0
        List<OrderInvestDetailDO> buildFailedOrderInvest = this.buildFailedOrderInvestDetail(orderInvestDetailDOList);
        if (CollectionUtils.isNotEmpty(buildFailedOrderInvest) && buildFailedOrderInvest.size() ==  orderInvestDetailDOList.size()) {
            buildFailedOrderInvest.sort(( o1, o2) -> o1.getVoidDate().compareTo(o2.getVoidDate()));
            orderInfoListBO.setPricedDate(buildFailedOrderInvest.get(buildFailedOrderInvest.size() - 1).getVoidDate());
            orderInfoListBO.setCompletedDate(buildFailedOrderInvest.get(buildFailedOrderInvest.size() - 1).getVoidDate());
            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
        }

        //如果详情都为失败状态，手续费为0
//        if (this.orderInvestDetailAllFail(buildCompletedOrderInvest)) {
//            orderInfoListBO.setTransactionCharge(BigDecimal.ZERO);
//            orderInfoListBO.setOrderStatus(OrderDetailStatusEnum.FAIL.getCode());
//        }
        return orderInfoListBO;
    }

    private List<OrderInvestDetailDO> buildFailedOrderInvestDetail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        List<OrderInvestDetailDO> result = Lists.newArrayList();
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //交易失败
            if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date voidDate = orderInvestDetailDO.getVoidDate();
                if (voidDate != null) {
                    result.add(orderInvestDetailDO);
                }
            }else {
                return null;
            }
        }
        return result;
    }

    private List<OrderInvestDetailDO> bulidApplySuccessOrderInvestDetail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        List<OrderInvestDetailDO> result = Lists.newArrayList();
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //下单成功,确认中,确认成功
            if (transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.RECEIVING.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()) {
                Date expectedConfirmedDate = orderInvestDetailDO.getExpectedConfirmedDate();
                if (expectedConfirmedDate != null) {
                    result.add(orderInvestDetailDO);
                }
            }else {
                return null;
            }
        }
        return result;
    }

    private Boolean orderInvestDetailAllFail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        if (CollectionUtils.isEmpty(orderInvestDetailDOList)) {
            return false;
        }
        Boolean flag = true;
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null || transactionStatus.intValue() != TransactionStatusEnum.FAIL.getCode()) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    private List<OrderInvestDetailDO> buildPricedOrderInvestDetail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        List<OrderInvestDetailDO> result = Lists.newArrayList();
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                //continue;
                return null;
            }
            //transactionStatus.intValue() == TransactionStatusEnum.RECEIVED.getCode() ||
            //部分成功
            if (transactionStatus.intValue() == TransactionStatusEnum.PRICED.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
//                    || transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date pricedDate = orderInvestDetailDO.getPricedDate();
                if (pricedDate != null) {
                    result.add(orderInvestDetailDO);
                }
            }/*else {
                return null;
            }*/
        }
        return result;
    }

    private List<OrderInvestDetailDO> buildCompletedOrderInvestDetail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        List<OrderInvestDetailDO> result = Lists.newArrayList();
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            //|| transactionStatus.intValue() == TransactionStatusEnum.CANCELED.getCode()
            if (transactionStatus.intValue() == TransactionStatusEnum.SUCCESS.getCode()
                    || transactionStatus.intValue() == TransactionStatusEnum.SUCCESS_PART.getCode()) {
                Date completedDate = orderInvestDetailDO.getCompletedDate();
                if (completedDate != null) {
                    result.add(orderInvestDetailDO);
                }
            } /*else if (transactionStatus.intValue() == TransactionStatusEnum.FAIL.getCode()) {
                Date completedDate = orderInvestDetailDO.getVoidDate();
                if (completedDate != null) {
                    orderInvestDetailDO.setCompletedDate(completedDate);
                    result.add(orderInvestDetailDO);
                }
            } else if (transactionStatus.intValue() == TransactionStatusEnum.PAY_FAIL.getCode()) {
                Date completedDate = orderInvestDetailDO.getCreateTime();
                if (completedDate != null) {
                    orderInvestDetailDO.setCompletedDate(completedDate);
                    result.add(orderInvestDetailDO);
                }
            }else {
                return null;
            }*/
        }
        return result;
    }

    private List<OrderInvestDetailDO> buildPayFailOrderInvestDetail(List<OrderInvestDetailDO> orderInvestDetailDOList) {
        List<OrderInvestDetailDO> result = Lists.newArrayList();
        for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {
            Integer transactionStatus = orderInvestDetailDO.getTransactionStatus();
            if (transactionStatus == null) {
                return null;
            }
            if (transactionStatus.intValue() == TransactionStatusEnum.PAY_FAIL.getCode()) {
//                Date completedDate = orderInvestDetailDO.getCreateTime();
//                if (completedDate != null) {
//                    orderInvestDetailDO.setCompletedDate(completedDate);
//                    result.add(orderInvestDetailDO);
//                }

                result.add(orderInvestDetailDO);
            } else {
                return null;
            }
        }
        return result;
    }


    @Override
    public List<OrderInvestBO> querySyncOrderList(String businessCode, Integer orderStatus, Integer channel) {
        return orderInvestMapper.querySyncOrderList(businessCode.split(","), orderStatus, channel);
    }

    @Event(reliability = false,
            eventType = "",
            eventId = "",
            queue = "",
            exchange = "exchange.investOrder.syncComplete",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void syncInvestOrder(OrderInvestBO orderInvestBO, SyncOrderListResponse syncOrderListResponse) {

        //order_info状态
        OrderStatusEnum orderStatus = null;
        //order_invest 手续费
        BigDecimal allTransactionCharge = BigDecimal.ZERO;
        //失败手续费
        BigDecimal failedTransactionCharge = BigDecimal.ZERO;
        //实际交易金额
        BigDecimal allTransactionAmount = BigDecimal.ZERO;
        //交易失败金额
        BigDecimal failedTransactionAmount = BigDecimal.ZERO;
        //交易成功金额
        BigDecimal successTransactionAmount = BigDecimal.ZERO;
        //交易成功手续费
        BigDecimal successTransactionCharge = BigDecimal.ZERO;
        //是否全部失败
        Boolean allFailed = Boolean.FALSE, allSuccess = Boolean.FALSE;
        if (!Objects.isNull(syncOrderListResponse) && CollectionUtils.isNotEmpty(syncOrderListResponse.getData())) {
            //有数据默认处理中
            orderStatus = OrderStatusEnum.PROCESS;
            //完成状态订单的数量,下面四种状态为订单详情终态，如果订单明细所有状态为终态，则order_Info为完成状态
            //TransactionStatusEnum.SUCCESS,TransactionStatusEnum.FAIL,TransactionStatusEnum.CANCELED,TransactionStatusEnum.PAY_FAIL
            int completeDetailNum = 0, failedDetailNum = 0, successNum = 0;
            List<SyncOrderResponse> syncOrderResponseList = syncOrderListResponse.getData();
            for (SyncOrderResponse syncOrderResponse : syncOrderResponseList) {

                OrderInvestDetailDO orderInvestDetailDO = new OrderInvestDetailDO();
                orderInvestDetailDO.setUserId(orderInvestBO.getUserId());
                orderInvestDetailDO.setOrderInvestId(orderInvestBO.getOrderInvestId());
                orderInvestDetailDO.setMerchantNumber(orderInvestBO.getMerchantNumber());
                orderInvestDetailDO.setContractNumber(orderInvestBO.getMerchantNumber());//TODO
                orderInvestDetailDO.setFundCode(syncOrderResponse.getFundCode());
                orderInvestDetailDO.setFundName(syncOrderResponse.getFundName());
                orderInvestDetailDO.setCancelEnable(syncOrderResponse.getCancelEnable());
                Date cancelDate = StringUtils.isEmpty(syncOrderResponse.getCanceledDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getCanceledDate())).toDate();
                orderInvestDetailDO.setCanceledDate(cancelDate);
                orderInvestDetailDO.setBankCode(syncOrderResponse.getBankCode());
                orderInvestDetailDO.setBankNumber(syncOrderResponse.getBankNumber());
                orderInvestDetailDO.setTransactionAmount(new BigDecimal(syncOrderResponse.getTransactionAmount()));
                orderInvestDetailDO.setTransactionUnit(new BigDecimal(syncOrderResponse.getTransactionUnit()));
                orderInvestDetailDO.setTransactionRate(new BigDecimal(syncOrderResponse.getTransactionRate()));
                orderInvestDetailDO.setDiscountRate(new BigDecimal(syncOrderResponse.getDiscountRate()));
                orderInvestDetailDO.setTransactionCharge(new BigDecimal(syncOrderResponse.getTransactionCharge()));
                orderInvestDetailDO.setDiscountTransactionCharge(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
                Date orderDate = StringUtils.isEmpty(syncOrderResponse.getOrderDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getOrderDate())).toDate();
                orderInvestDetailDO.setOrderDate(orderDate);
                orderInvestDetailDO.setTransactionCfmLagDay(Integer.valueOf(syncOrderResponse.getTransactionCfmLagDay() == null ? "0" : syncOrderResponse.getTransactionCfmLagDay()));
                Date transactionDate = StringUtils.isEmpty(syncOrderResponse.getTransactionDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getTransactionDate())).toDate();
                orderInvestDetailDO.setTransactionDate(transactionDate);
                orderInvestDetailDO.setTransactionPrice(new BigDecimal(syncOrderResponse.getTransactionPrice()));
                Date pricedDate = StringUtils.isEmpty(syncOrderResponse.getPricedDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getPricedDate())).toDate();
                orderInvestDetailDO.setPricedDate(pricedDate);
                Date expConfirmeDate = StringUtils.isEmpty(syncOrderResponse.getExpectedConfirmedDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getExpectedConfirmedDate())).toDate();
                orderInvestDetailDO.setExpectedConfirmedDate(expConfirmeDate);
                Date completedDate = StringUtils.isEmpty(syncOrderResponse.getCompletedDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getCompletedDate())).toDate();
                orderInvestDetailDO.setCompletedDate(completedDate);
                Date settlementDate = StringUtils.isEmpty(syncOrderResponse.getSettlementDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getSettlementDate())).toDate();
                orderInvestDetailDO.setSettlementDate(settlementDate);
                TransactionStatusEnum transactionStatusEnum =
                        TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
                orderInvestDetailDO.setTransactionStatus(transactionStatusEnum.getCode());

                orderInvestDetailDO.setInvestorPayId(Integer.parseInt(syncOrderResponse.getInvestorPayId()));
                orderInvestDetailDO.setReason(syncOrderResponse.getReason());
                orderInvestDetailDO.setRspId(Integer.parseInt(syncOrderResponse.getRspId() == null ? "0" : syncOrderResponse.getRspId()));
                orderInvestDetailDO.setTransactionType(syncOrderResponse.getTransactionType());
                orderInvestDetailDO.setPortfolioCode(orderInvestBO.getPortfolioCode());
                orderInvestDetailDO.setPortfolioId(syncOrderResponse.getPortfolioId());
                Date voidDate = StringUtils.isEmpty(syncOrderResponse.getVoidDate()) ? null : new DateTime(Long.valueOf(syncOrderResponse.getVoidDate())).toDate();
                orderInvestDetailDO.setVoidDate(voidDate);


                //查询订单详情是否存在，存在更新，不存在插入，兼容买入接口无响应情况
                //根据主订单ID和fund_code查询订单详情
                OrderInvestDetailDO queryDO = new OrderInvestDetailDO();
                queryDO.setOrderInvestId(orderInvestBO.getOrderInvestId());
                queryDO.setFundCode(syncOrderResponse.getFundCode());
                OrderInvestDetailDO detailDO = orderInvestDetailMapper.queryInvestDetail(queryDO);
                if (Objects.isNull(detailDO)) {
                    //保存
                    orderInvestDetailDO.setOrderInvestDetailId(guidCreater.getUniqueID());
                    orderInvestDetailMapper.insert(orderInvestDetailDO);
                } else {
                    //更新
                    orderInvestDetailDO.setOrderInvestDetailId(detailDO.getOrderInvestDetailId());
                    orderInvestDetailMapper.update(orderInvestDetailDO);
                }
                if (transactionStatusEnum == TransactionStatusEnum.SUCCESS || transactionStatusEnum == TransactionStatusEnum.FAIL
                        || transactionStatusEnum == TransactionStatusEnum.CANCELED || transactionStatusEnum == TransactionStatusEnum.PAY_FAIL) {
                    completeDetailNum++;
                }

                if (transactionStatusEnum == TransactionStatusEnum.FAIL || transactionStatusEnum == TransactionStatusEnum.PAY_FAIL) {
                    failedDetailNum++;
                    failedTransactionAmount = failedTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                    failedTransactionCharge = failedTransactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
                }

                if (transactionStatusEnum == TransactionStatusEnum.SUCCESS) {
                    successNum++;
                    successTransactionAmount = successTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                    successTransactionCharge = successTransactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
                }

                //手续费
                allTransactionCharge = allTransactionCharge.add(orderInvestDetailDO.getDiscountTransactionCharge());
                allTransactionAmount = allTransactionAmount.add(orderInvestDetailDO.getTransactionAmount());
            }

            if (completeDetailNum == syncOrderListResponse.getData().size()) {
                //全部终态
                orderStatus = OrderStatusEnum.COMPLETE;
            }
            allFailed = failedDetailNum == syncOrderListResponse.getData().size();
            allSuccess = successNum == syncOrderListResponse.getData().size();
        }

        //订单表中的金额和手续费
        BigDecimal transactionAmount = BigDecimal.ZERO;
        BigDecimal transactionCharge = BigDecimal.ZERO;

        TransactionStatusEnum transactionStatusEnum = null;

        //处理上层订单状态
        //order_info状态
        if (orderStatus != null) {
            if (orderStatus == OrderStatusEnum.COMPLETE) {
                //订单完成
                orderInfoMapper.update(new OrderInfoDO(orderInvestBO.getOrderNo(), OrderStatusEnum.COMPLETE.getCode()));
                //全部失败
                if (allFailed) {
                    transactionAmount = failedTransactionAmount;
                    transactionCharge = failedTransactionCharge;
                    transactionStatusEnum = TransactionStatusEnum.FAIL;
                } else if (allSuccess) {
                    transactionStatusEnum = TransactionStatusEnum.SUCCESS;
                    transactionAmount = successTransactionAmount;
                    transactionCharge = successTransactionCharge;
                } else {
                    //非全部失败，只显示成功部分
                    transactionAmount = successTransactionAmount;
                    transactionCharge = successTransactionCharge;
                    transactionStatusEnum = TransactionStatusEnum.SUCCESS_PART;
                }

            } else {
                orderInfoMapper.updateOrderStatus(orderInvestBO.getOrderNo(), orderStatus.getCode());
                //未完成，订单金额和手续费，为完整金额
                transactionAmount = allTransactionAmount;
                transactionCharge = allTransactionCharge;
            }
        }
        //order_invest手续费
        OrderInvestDO investDO = new OrderInvestDO();

        investDO.setTransactionAmount(transactionAmount);
        investDO.setTransactionCharge(transactionCharge);
        investDO.setOrderInvestId(orderInvestBO.getOrderInvestId());
        if (transactionStatusEnum != null) {
            investDO.setTransactionStatus(transactionStatusEnum.getCode());
        }

        orderInvestMapper.updateOrderInvest(investDO);

        //每次同步完成发消息
        InvestOrderSyncCompleteMessage message = new InvestOrderSyncCompleteMessage();
        message.setUserId(orderInvestBO.getUserId());
        message.setOrderNo(orderInvestBO.getOrderNo());
        message.setOrderInvestId(orderInvestBO.getOrderInvestId());
        message.setAccountNumber(orderInvestBO.getAccountNumber());
        message.setMerchantNumber(orderInvestBO.getMerchantNumber());
        message.setInvestType(orderInvestBO.getInvestType());

        EventMessageContext.addMessage(message);

        // 若是自动定投，发出重新鉴权queue
        Integer investType = orderInvestBO.getInvestType();
        if (null == investType) {
            log.info("orderInvest类型为空", orderInvestBO);
        }
        if (InvestTypeEnum.AUTO_INVEST.getType().equals(investType)) {
            sendReAuthenticationMsg(orderInvestBO.getAccountNumber(), orderInvestBO.getUserId(), allTransactionAmount);
        }

    }

    @Event(reliability = true,
            eventType = "'autoInvestComplete'",
            eventId = "#message.getOrderNo()",
            queue = "",
            exchange = "exchange.autoInvest.complete",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public OrderInvestDO afterAutoInvest(PlanInfoDO planInfoDO, OrderInvestDO orderInvestDO, SyncOrderListResponse syncOrderListResponse) throws Exception {

        //获取响应，保存订单详情，更新订单金额
        BigDecimal transactionAmount = BigDecimal.ZERO;
        //实际交易费用，如果订单未完成，返回预估费用
        BigDecimal transactionCharge = BigDecimal.ZERO;
        String thirdPortfolioId = "";
        int completeNum = 0;
        Long orderInvestId = guidCreater.getUniqueID();
        List<OrderInvestDetailDO> orderInvestDetailDOList = Lists.newArrayList();
        for (SyncOrderResponse syncOrderResponse : syncOrderListResponse.getData()) {

            OrderInvestDetailDO orderInvestDetailDO = new OrderInvestDetailDO();
            orderInvestDetailDO.setOrderInvestDetailId(guidCreater.getUniqueID());
            orderInvestDetailDO.setUserId(orderInvestDO.getUserId());
            orderInvestDetailDO.setOrderInvestId(orderInvestId);
            orderInvestDetailDO.setMerchantNumber(syncOrderResponse.getMerchantNumber());
            orderInvestDetailDO.setContractNumber(syncOrderResponse.getContractNumber());
            orderInvestDetailDO.setFundCode(syncOrderResponse.getFundCode());
            orderInvestDetailDO.setFundName(syncOrderResponse.getFundName());
            orderInvestDetailDO.setCancelEnable(syncOrderResponse.getCancelEnable());
            if (StringUtils.isNotBlank(syncOrderResponse.getCanceledDate())) {
                orderInvestDetailDO.setCanceledDate(new DateTime(Long.valueOf(syncOrderResponse.getCanceledDate())).toDate());
            }

            orderInvestDetailDO.setBankCode(syncOrderResponse.getBankCode());
            orderInvestDetailDO.setBankNumber(syncOrderResponse.getBankNumber());
            orderInvestDetailDO.setTransactionAmount(new BigDecimal(syncOrderResponse.getTransactionAmount()));
            orderInvestDetailDO.setTransactionUnit(new BigDecimal(syncOrderResponse.getTransactionUnit()));
            orderInvestDetailDO.setTransactionRate(new BigDecimal(syncOrderResponse.getTransactionRate()));
            orderInvestDetailDO.setDiscountRate(new BigDecimal(syncOrderResponse.getDiscountRate()));
            orderInvestDetailDO.setTransactionCharge(new BigDecimal(syncOrderResponse.getTransactionCharge()));
            orderInvestDetailDO.setDiscountTransactionCharge(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));
            if (StringUtils.isNotBlank(syncOrderResponse.getOrderDate())) {
                orderInvestDetailDO.setOrderDate(new DateTime(Long.valueOf(syncOrderResponse.getOrderDate())).toDate());
            }
            if (StringUtils.isNotBlank(syncOrderResponse.getTransactionCfmLagDay())) {
                orderInvestDetailDO.setTransactionCfmLagDay(Integer.valueOf(syncOrderResponse.getTransactionCfmLagDay()));
            }

            if (StringUtils.isNotBlank(syncOrderResponse.getTransactionDate())) {
                orderInvestDetailDO.setTransactionDate(new DateTime(Long.valueOf(syncOrderResponse.getTransactionDate())).toDate());
            }

            orderInvestDetailDO.setTransactionPrice(new BigDecimal(syncOrderResponse.getTransactionPrice()));

            if (StringUtils.isNotBlank(syncOrderResponse.getPricedDate())) {
                orderInvestDetailDO.setPricedDate(new DateTime(Long.valueOf(syncOrderResponse.getPricedDate())).toDate());
            }

            if (StringUtils.isNotBlank(syncOrderResponse.getExpectedConfirmedDate())) {
                orderInvestDetailDO.setExpectedConfirmedDate(new DateTime(Long.valueOf(syncOrderResponse.getExpectedConfirmedDate())).toDate());
            }

            if (StringUtils.isNotBlank(syncOrderResponse.getCompletedDate())) {
                orderInvestDetailDO.setCompletedDate(new DateTime(Long.valueOf(syncOrderResponse.getCompletedDate())).toDate());
            }

            if (StringUtils.isNotBlank(syncOrderResponse.getSettlementDate())) {
                orderInvestDetailDO.setSettlementDate(new DateTime(Long.valueOf(syncOrderResponse.getSettlementDate())).toDate());
            }

            TransactionStatusEnum transactionStatusEnum =
                    TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, syncOrderResponse.getTransactionStatus());
            orderInvestDetailDO.setTransactionStatus(transactionStatusEnum.getCode());

            orderInvestDetailDO.setInvestorPayId(Integer.valueOf(syncOrderResponse.getInvestorPayId()));
            orderInvestDetailDO.setReason(syncOrderResponse.getReason());
            orderInvestDetailDO.setRspId(Integer.valueOf(planInfoDO.getThirdPlanId()));
            //orderInvestDetailDO.setRspId(Integer.valueOf(syncOrderResponse.getRspId()));
            orderInvestDetailDO.setTransactionType(syncOrderResponse.getTransactionType());
            orderInvestDetailDO.setPortfolioCode(orderInvestDO.getPortfolioCode());
            orderInvestDetailDO.setPortfolioId(syncOrderResponse.getPortfolioId());
            if (StringUtils.isNotEmpty(syncOrderResponse.getVoidDate())) {
                orderInvestDetailDO.setVoidDate(new DateTime(Long.valueOf(syncOrderResponse.getVoidDate())).toDate());
            }

            orderInvestDetailDOList.add(orderInvestDetailDO);

            //交易金额和手续费仅保存成功订单的数据
            if (transactionStatusEnum == TransactionStatusEnum.SUCCESS) {
                transactionAmount = transactionAmount.add(new BigDecimal(syncOrderResponse.getTransactionAmount()));
            }
            //订单完成之前，取所有手续费
            transactionCharge = transactionCharge.add(new BigDecimal(syncOrderResponse.getDiscountTransactionCharge()));

            if (StringUtils.isEmpty(thirdPortfolioId)) {
                //取一个持仓id
                thirdPortfolioId = syncOrderResponse.getPortfolioId();
            }
            //订单完成数量
            if (transactionStatusEnum == TransactionStatusEnum.SUCCESS || transactionStatusEnum == TransactionStatusEnum.FAIL
                    || transactionStatusEnum == TransactionStatusEnum.CANCELED || transactionStatusEnum == TransactionStatusEnum.PAY_FAIL) {
                completeNum++;
            }
        }

        //保存order_info
        OrderStatusEnum orderStatus = OrderStatusEnum.PROCESS;
        //定投回调的订单，初始化都是处理中订单
        /*if (completeNum == syncOrderListResponse.getData().size()) {
            //全部终态
            orderStatus = OrderStatusEnum.COMPLETE;
        }*/
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        orderInfoDO.setUserId(orderInvestDO.getUserId());
        orderInfoDO.setOrderNo(guidCreater.getUniqueID());
        orderInfoDO.setOrderStatus(orderStatus.getCode());
        orderInfoDO.setBusinessCode(OrderBusinessEnum.AUTO_INVEST.getCode());
        orderInfoDO.setSource("");
        //定投金额
        orderInfoDO.setTransactionAmount(planInfoDO.getPortfolioAmount());
        orderInfoDO.setSendTime(new Date());
        orderInfoDO.setChannel(orderInvestDO.getChannel());
        orderInfoMapper.insert(orderInfoDO);

        //保存order_invest
        orderInvestDO.setOrderInvestId(orderInvestId);
        //默认为发起金额
        orderInvestDO.setTransactionAmount(planInfoDO.getPortfolioAmount());//交易金额
        orderInvestDO.setTransactionCharge(transactionCharge);//手续费
        orderInvestDO.setThirdPortfolioId(thirdPortfolioId);//持仓id

        orderInvestDO.setOrderNo(orderInfoDO.getOrderNo());
        orderInvestDO.setTransactionStatus(0);//TODO
        orderInvestDO.setResponseCode("");
        orderInvestDO.setResponseMessage("");

        orderInvestMapper.insert(orderInvestDO);

        //保存订单详情
        orderInvestDetailMapper.insertBatch(orderInvestDetailDOList);


        //发消息
        AutoInvestCompleteMessage autoInvestCompleteMessage = new AutoInvestCompleteMessage();
        autoInvestCompleteMessage.setUserId(planInfoDO.getUserId());
        autoInvestCompleteMessage.setPlanInfoId(planInfoDO.getPlanInfoId());
        autoInvestCompleteMessage.setOrderInvestId(orderInvestDO.getOrderInvestId());
        autoInvestCompleteMessage.setOrderNo(orderInfoDO.getOrderNo());
        autoInvestCompleteMessage.setTransactionAmount(transactionAmount);
        autoInvestCompleteMessage.setThirdPortfolioId(thirdPortfolioId);
        autoInvestCompleteMessage.setAccountNumber(orderInvestDO.getAccountNumber());

        EventMessageContext.addMessage(autoInvestCompleteMessage);

        sendReAuthenticationMsg(orderInvestDO.getAccountNumber(), orderInvestDO.getUserId(), planInfoDO.getPortfolioAmount());
        orderInvestDO.setThirdPortfolioId(thirdPortfolioId);
        orderInvestDO.setTransactionAmount(transactionAmount);
        orderInvestDO.setTransactionCharge(transactionCharge);

        return orderInvestDO;
    }


    private void sendReAuthenticationMsg(String accountNumber, String userId, BigDecimal amount) {
        ReAuthenticationMessage reAuthenticationMessage = new ReAuthenticationMessage();
        reAuthenticationMessage.setAccountNumber(accountNumber);
        reAuthenticationMessage.setUserId(userId);
        reAuthenticationMessage.setChannel(FundChannelEnum.YIFENG);
        reAuthenticationMessage.setInvestAmount(amount);
//        EventMessageContext.addMessage(reAuthenticationMessage);
        //jlc mq 框架不支持发多次消息场景, 采用 spring amqptemplate 发送
        amqpTemplate.convertAndSend("exchange.bankcard.reauth", null, reAuthenticationMessage);
    }

    /**
     * 计算订单预计确认日期（订单明细中最大的预计确认日期（交易日期+基金申购确认日差））
     *
     * @author yunpeng.zhang
     */
    @Override
    public Date getOrderExpectedConfirmDate(Long orderInvestId) {
        OrderInvestDetailRequest condition = new OrderInvestDetailRequest();
        condition.setOrderInvestId(orderInvestId);
        List<OrderInvestDetailDO> orderInvestDetailList = orderInvestDetailMapper.listByOrderInvestDetailCondition(condition);

        LocalDate exceptedConfirmLocalDate = null;
        LocalDate transactionLocalDate;
        Period plusDays;
        LocalDate resultDate;

        if (CollectionUtils.isNotEmpty(orderInvestDetailList)) {
            for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailList) {
                // 基金交易日期
                Integer transactionCfmLagDay = orderInvestDetailDO.getTransactionCfmLagDay();
                // 基金申购确认日差
                Date transactionDate = orderInvestDetailDO.getTransactionDate();

                transactionLocalDate = DateTimeUtil.toLocalDate(transactionDate);
                plusDays = Period.ofDays(transactionCfmLagDay);
                resultDate = transactionLocalDate.plus(plusDays);
                if (exceptedConfirmLocalDate == null) {
                    exceptedConfirmLocalDate = resultDate;
                } else {
                    exceptedConfirmLocalDate = resultDate.compareTo(exceptedConfirmLocalDate) > 0 ? resultDate : exceptedConfirmLocalDate;
                }
            }
        }
        if (exceptedConfirmLocalDate == null) {
            throw new RuntimeException("获取订单预计确认日期失败，买入订单id：" + orderInvestId);
        }

        return DateTimeUtil.toDate(exceptedConfirmLocalDate);
    }

    @Override
    public OrderInvestDO queryByMerchantNumber(String merchantNumber, FundChannelEnum channel) {
        return orderInvestMapper.getByMerchantNumberAndChannel(merchantNumber, channel.getChannel());
    }

    @Override
    public OrderInvestDO queryByOrderInvestId(Long orderInvestId) {
        return orderInvestMapper.getByOrderInvestId(orderInvestId);
    }

    @Event(reliability = true,
            eventType = "'autoInvestCallbackCompensate'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.autoInvest.callBack.compensate",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void compensateAutoInvestCallback(AutoInvestMessage autoInvestMessage) throws Exception {

        autoInvestMessage.setEventId(autoInvestMessage.getThirdPlanId() + "_" + autoInvestMessage.getMerchantNumber());

        EventMessageContext.addMessage(autoInvestMessage);
    }

    @Event(reliability = true,
            eventType = "'investApply'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.invest.apply",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void afterFundInvestApply(OrderInvestDO orderInvestDO, BaseResponse<YfFundInvestResponse> baseResponse) {
        if (!Objects.isNull(baseResponse) && !Objects.isNull(baseResponse.getData())) {
            List<YfFundInvestResponseDetail> yfFundInvestResponseDetailList = (List<YfFundInvestResponseDetail>) baseResponse.getData();
            List<OrderInvestDetailDO> orderInvestDetailDOS = this.buildFundInvestDetail(yfFundInvestResponseDetailList, orderInvestDO);
            orderInvestDetailMapper.insertBatch(orderInvestDetailDOS);
            //更新订单交易金额和手续费
            OrderInvestDO param = new OrderInvestDO();
            param.setOrderInvestId(orderInvestDO.getOrderInvestId());
            param.setTransactionAmount(orderInvestDO.getTransactionAmount());
            param.setTransactionCharge(orderInvestDO.getTransactionCharge());
            orderInvestMapper.updateOrderInvest(param);
        }

        InvestOrderMessage message = new InvestOrderMessage();
        message.setUserId(orderInvestDO.getUserId());
        message.setAccountNumber(orderInvestDO.getAccountNumber());
        message.setOrderNo(orderInvestDO.getOrderNo());
        message.setMerchantNumber(orderInvestDO.getMerchantNumber());
        message.setEventId(orderInvestDO.getUserId() + orderInvestDO.getOrderNo());

        EventMessageContext.addMessage(message);
    }

    @Override
    public Long countUserInvestOrder(String userId, InvestTypeEnum investTypeEnum) {
        return orderInvestMapper.count(userId, investTypeEnum.getType());
    }


    private List<OrderInvestDetailDO> buildFundInvestDetail(List<YfFundInvestResponseDetail> yfFundInvestResponseDetailList, OrderInvestDO orderInvestDO) {
        List<OrderInvestDetailDO> orderInvestDetailDOList = Lists.newArrayList();
        BigDecimal transactionAmount = BigDecimal.ZERO;
        BigDecimal transactionCharge = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(yfFundInvestResponseDetailList)) {
            for (YfFundInvestResponseDetail detail : yfFundInvestResponseDetailList) {
                OrderInvestDetailDO orderInvestDetailDO = new OrderInvestDetailDO();
                orderInvestDetailDO.setOrderInvestDetailId(guidCreater.getUniqueID());
                orderInvestDetailDO.setUserId(orderInvestDO.getUserId());
                //投资订单表id
                orderInvestDetailDO.setOrderInvestId(orderInvestDO.getOrderInvestId());
                //奕丰订单号
                orderInvestDetailDO.setContractNumber(detail.getContractNumber());
                //交易订单号
                orderInvestDetailDO.setMerchantNumber(orderInvestDO.getMerchantNumber());
                //基金代码
                orderInvestDetailDO.setFundCode(detail.getFundCode());
                //基金名称
                orderInvestDetailDO.setFundName(detail.getFundName());
                //交易金额
                orderInvestDetailDO.setTransactionAmount(detail.getInvestmentAmount());
                //下单日期
                if (detail.getOrderDate() != null) {
                    orderInvestDetailDO.setOrderDate(new DateTime(detail.getOrderDate()).toDate());
                }
                // 预计确认日期
                orderInvestDetailDO.setExpectedConfirmedDate(DateTimeUtil.parseDate(detail.getExpectedConfirmedDate()));
                //基金组合代码
                orderInvestDetailDO.setPortfolioCode(orderInvestDO.getPortfolioCode());
                //基金持仓id
                orderInvestDetailDO.setPortfolioId(orderInvestDO.getThirdPortfolioId());
                //交易状态
                TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, detail.getTransactionStatus());
                orderInvestDetailDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
                //交易日期
                if (detail.getTransactionDate() != null) {
                    orderInvestDetailDO.setTransactionDate(new DateTime(detail.getTransactionDate()).toDate());
                }
                //失败原因
                orderInvestDetailDO.setReason(detail.getReason());
                orderInvestDetailDO.setTransactionType(detail.getTransactionType());
                orderInvestDetailDO.setCreateTime(new Date());
                orderInvestDetailDO.setUpdateTime(new Date());
                orderInvestDetailDO.setYn(0);
                transactionAmount = transactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                orderInvestDetailDOList.add(orderInvestDetailDO);
            }
        }
        orderInvestDO.setTransactionAmount(transactionAmount);
        orderInvestDO.setTransactionCharge(transactionCharge);
        return orderInvestDetailDOList;
    }

    private List<OrderInvestDetailDO> buildInvestDetail(YfInvestResponse yfInvestResponse, OrderInvestDO orderInvestDO) {
        List<OrderInvestDetailDO> orderInvestDetailDOS = Lists.newArrayList();
        List<YfInvestResponseDetail> details = yfInvestResponse.getDetails();
        BigDecimal transactionAmount = BigDecimal.ZERO;
        BigDecimal transactionCharge = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(details)) {
            for (YfInvestResponseDetail detail : details) {
                OrderInvestDetailDO orderInvestDetailDO = new OrderInvestDetailDO();
                orderInvestDetailDO.setOrderInvestDetailId(guidCreater.getUniqueID());
                orderInvestDetailDO.setUserId(orderInvestDO.getUserId());
                //投资订单表id
                orderInvestDetailDO.setOrderInvestId(orderInvestDO.getOrderInvestId());
                //奕丰订单号
                orderInvestDetailDO.setContractNumber(detail.getContractNumber());
                //交易订单号
                orderInvestDetailDO.setMerchantNumber(orderInvestDO.getMerchantNumber());
                //基金代码
                orderInvestDetailDO.setFundCode(detail.getFundCode());
                //基金名称
                orderInvestDetailDO.setFundName(detail.getFundName());
                //交易金额
                orderInvestDetailDO.setTransactionAmount(detail.getInvestmentAmount());
                //下单日期
                if (StringUtils.isNotBlank(detail.getOrderDate())) {
                    orderInvestDetailDO.setOrderDate(new DateTime(Long.valueOf(detail.getOrderDate())).toDate());
                }
                //基金组合代码
                orderInvestDetailDO.setPortfolioCode(orderInvestDO.getPortfolioCode());
                //基金持仓id
                orderInvestDetailDO.setPortfolioId(orderInvestDO.getThirdPortfolioId());
                //交易状态
                TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, detail.getTransactionStatus());
                orderInvestDetailDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
                //交易日期
                if (StringUtils.isNotBlank(detail.getTransactionDate())) {
                    orderInvestDetailDO.setTransactionDate(new DateTime(Long.valueOf(detail.getTransactionDate())).toDate());
                }
                //失败原因
                orderInvestDetailDO.setReason(detail.getReason());
                //确认日差
                orderInvestDetailDO.setTransactionCfmLagDay(detail.getTransactionCfmLagDay() == null ? Integer.valueOf(0) : detail.getTransactionCfmLagDay());
                orderInvestDetailDO.setTransactionType(detail.getTransactionType());
                orderInvestDetailDO.setCreateTime(new Date());
                orderInvestDetailDO.setUpdateTime(new Date());
                orderInvestDetailDO.setYn(0);

                transactionAmount = transactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                orderInvestDetailDOS.add(orderInvestDetailDO);
            }
        }
        orderInvestDO.setTransactionAmount(transactionAmount);
        orderInvestDO.setTransactionCharge(transactionCharge);

        return orderInvestDetailDOS;
    }
}