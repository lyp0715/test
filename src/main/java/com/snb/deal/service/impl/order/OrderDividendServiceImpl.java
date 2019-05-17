package com.snb.deal.service.impl.order;

import com.google.common.base.Preconditions;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.bo.order.OrderDividendBO;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderDividendDO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.enums.OrderBusinessEnum;
import com.snb.deal.enums.TransactionStatusEnum;
import com.snb.deal.mapper.order.OrderDividendMapper;
import com.snb.deal.mapper.order.OrderInfoMapper;
import com.snb.deal.service.order.OrderDividendService;
import com.snb.third.yifeng.dto.order.YfOrderInfo;
import com.snb.third.yifeng.dto.order.YfQueryOrderListResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class OrderDividendServiceImpl implements OrderDividendService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDividendMapper orderDividendMapper;
    @Autowired
    private GuidCreater guidCreater;

    @Transactional
    @Override
    public void createDividendOrder(OrderDividendBO orderDividendBO, YfQueryOrderListResponse yfQueryOrderListResponse) throws Exception {
        log.info("创建红利再投订单开始-forceOrderBO:{}", orderDividendBO.toString());
        for (YfOrderInfo yfOrderInfo : yfQueryOrderListResponse.getData()) {
            if(StringUtils.isEmpty(yfOrderInfo.getMerchantNumber())){
                log.info("红利再投订单商户订单流水号为空-yfOrderInfo:{}", yfOrderInfo.toString());
                continue;
            }
            List<OrderDividendDO> orderDividendDOList=orderDividendMapper.queryByMerchantNumber(yfOrderInfo.getMerchantNumber(),FundChannelEnum.YIFENG.getChannel());
            if(CollectionUtils.isNotEmpty(orderDividendDOList)){
                log.info("红利再投订单已经被处理过-yfOrderInfo:{}", yfOrderInfo.toString());
                return;
            }
            //插入订单主表
            OrderInfoDO orderInfoDO = OrderInfoDO.build(OrderInfoDO.OrderInfoBuildEnum.FORCE,OrderBusinessEnum.DIVIDEND);
            orderInfoDO.setOrderNo(guidCreater.getUniqueID());
            orderInfoDO.setUserId(orderDividendBO.getUserId());
            orderInfoDO.setTransactionAmount(BigDecimal.ZERO);
            orderInfoMapper.insert(orderInfoDO);
            log.info("创建红利再投订单主订单-orderInfoDO:{}", orderInfoDO.toString());

            //保存红利再投订单
            OrderDividendDO orderDividendDO = new OrderDividendDO();
            orderDividendDO.setOrderDividendId(guidCreater.getUniqueID());
            orderDividendDO.setUserId(orderDividendBO.getUserId());
            orderDividendDO.setContractNumber(StringUtils.EMPTY);
            orderDividendDO.setFundCode(StringUtils.defaultString(yfOrderInfo.getFundCode()));
            orderDividendDO.setFundName(StringUtils.defaultString(yfOrderInfo.getFundName()));
            orderDividendDO.setMerchantNumber(yfOrderInfo.getMerchantNumber());
            orderDividendDO.setAccountNumber(StringUtils.defaultString(orderDividendBO.getAccountNumber()));
            orderDividendDO.setOrderNo(orderInfoDO.getOrderNo());
            Date orderDate=new Date();
            if (StringUtils.isNotEmpty(yfOrderInfo.getOrderDate())) {
                orderDate=DateTimeUtil.parseDate(yfOrderInfo.getOrderDate());
            }
            orderDividendDO.setOrderDate(orderDate);
            orderDividendDO.setPricedDate(DateTimeUtil.parseDate(yfOrderInfo.getPricedDate()));
            TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.getTransactionStatus(FundChannelEnum.YIFENG, yfOrderInfo.getTransactionStatus());
            orderDividendDO.setTransactionStatus(transactionStatusEnum == null ? 0 : transactionStatusEnum.getCode());
            Date transactionDate=new Date();
            if (StringUtils.isNotEmpty(yfOrderInfo.getTransactionDate())) {
                transactionDate=DateTimeUtil.parseDate(yfOrderInfo.getTransactionDate());
            }
            orderDividendDO.setTransactionDate(transactionDate);
            orderDividendDO.setTransactionUnit(this.formatDecimal(yfOrderInfo.getTransactionUnit()));
            orderDividendDO.setThirdPortfolioId(StringUtils.defaultString(yfOrderInfo.getPortfolioId()));
            orderDividendDO.setChannel(orderDividendBO.getChannel());
            orderDividendMapper.insert(orderDividendDO);
            log.info("创建红利再投订单主订单-orderDividendDO:{}", orderDividendDO.toString());
        }
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

    @Override
    public OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO) {
        try {
            OrderInfoListBO orderInfoListBO = new OrderInfoListBO();
            OrderDividendDO orderDividendParam = new OrderDividendDO();
            orderDividendParam.setOrderNo(orderInfoDO.getOrderNo());
            OrderDividendDO orderDividend = orderDividendMapper.queryByOrderNo(orderDividendParam);
            Preconditions.checkNotNull(orderDividend);
            //拷贝银行卡号，银行卡名称，银行卡logo
            BeanUtils.copyProperties(orderListBO, orderInfoListBO);
            //设置交易份额
            BeanUtils.copyProperties(orderDividend, orderInfoListBO);
            //设置下单时间
            orderInfoListBO.setSendTime(orderInfoDO.getSendTime());
            //设置交易类型
            orderInfoListBO.setBusinessCode(orderInfoDO.getBusinessCode());
            return orderInfoListBO;
        } catch (Exception e) {
            log.error("getOrderDividendDTO is error,orderInfoDO:{},orderListBO:{}", orderInfoDO.toString(), orderListBO.toString(), e);
        }
        return null;
    }
}
