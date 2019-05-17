package com.snb.deal.service.impl.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.common.collect.Lists;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.order.OrderListDTO;
import com.snb.deal.api.dto.order.OrderListResponse;
import com.snb.deal.bo.order.OrderFeeRateBO;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.enums.OrderBusinessEnum;
import com.snb.deal.enums.OrderStatusEnum;
import com.snb.deal.mapper.order.*;
import com.snb.deal.service.order.*;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelDetailRequest;
import com.snb.fund.api.remote.FundMainModelRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/4/11 13:21
 */

@Slf4j
@Service
public class OrderInfoServiceImpl implements OrderInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;


    @Resource
    private OrderInvestMapper orderInvestMapper;

    @Resource
    private OrderRebalanceMapper orderRebalanceMapper;

    @Resource
    private OrderDividendMapper orderDividendMapper;

    @Resource
    private OrderRedeemService orderRedeemService;

    @Resource
    private OrderInvestService orderInvestService;

    @Resource
    private OrderRebalanceService rebalanceService;

    @Resource
    OrderRebalanceSummaryDetailMapper orderRebalanceSummaryDetailMapper;


    @Resource
    private OrderDividendService orderDividendService;

    @Reference(version = "1.0")
    private FundMainModelRemote fundMainModelRemote;


    @Override
    public OrderListResponse orderList(OrderListBO orderListBO) {
        OrderListResponse result = new OrderListResponse();

        OrderInfoDO orderInfoParam = new OrderInfoDO();
        orderInfoParam.setUserId(orderListBO.getUserId());
        orderInfoParam.setBusinessCode(orderListBO.getBusinessCode());
        orderInfoParam.setChannel(FundChannelEnum.YIFENG.getChannel());
        List<OrderInfoDO> orderInfoDOList = null;
        if (orderInfoParam.getBusinessCode() != null && (
                orderInfoParam.getBusinessCode().intValue() == OrderBusinessEnum.AUTO_INVEST.getCode()
                        || orderInfoParam.getBusinessCode().intValue() == OrderBusinessEnum.MANUL_INVEST.getCode())) {
            orderInfoDOList = orderInfoMapper.queryInvest(orderInfoParam);
        } else {
            orderInfoDOList = orderInfoMapper.query(orderInfoParam);
        }
        if (CollectionUtils.isEmpty(orderInfoDOList)) {
            return result;
        }
        List<OrderListDTO> orderListDTOList = Lists.newArrayList();
        for (OrderInfoDO orderInfoDO : orderInfoDOList) {
            if (orderInfoDO.getBusinessCode() == OrderBusinessEnum.AUTO_INVEST.getCode() ||
                    orderInfoDO.getBusinessCode() == OrderBusinessEnum.MANUL_INVEST.getCode()) {
                List<OrderInfoListBO> orderInfoListBOList = orderInvestService.getOrderList(orderInfoDO, orderListBO);
                if (CollectionUtils.isNotEmpty(orderInfoListBOList)) {
                    for (OrderInfoListBO orderInfoListBO : orderInfoListBOList) {
                        orderListDTOList.add(this.getOrderListDTO(orderInfoListBO));
                    }
                }
            } else if (orderInfoDO.getBusinessCode() == OrderBusinessEnum.REDEEM.getCode()) {
                OrderInfoListBO orderInfoListBO = orderRedeemService.getOrderList(orderInfoDO, orderListBO);
                if (orderInfoListBO != null) {
                    orderListDTOList.add(this.getOrderListDTO(orderInfoListBO));
                }
            } else if (orderInfoDO.getBusinessCode() == OrderBusinessEnum.REBALANCE.getCode()) {
                //对于订单状态为作废的订单不在订单列表中展示
                if (orderInfoDO.getOrderStatus() == null || orderInfoDO.getOrderStatus().intValue() == OrderStatusEnum.ABANDON.getCode()) {
                    continue;
                }
                OrderInfoListBO orderInfoListBO = rebalanceService.getOrderList(orderInfoDO, orderListBO);
                if (orderInfoListBO != null) {
                    orderListDTOList.add(this.getOrderListDTO(orderInfoListBO));
                }
            }
//            else if (orderInfoDO.getBusinessCode() == OrderBusinessEnum.DIVIDEND.getCode()) {
//                OrderInfoListBO orderInfoListBO = orderDividendService.getOrderList(orderInfoDO, orderListBO);
//                if (orderInfoListBO != null) {
//                    orderListDTOList.add(this.getOrderListDTO(orderInfoListBO));
//                }
//            }
        }
        //分页
        List<OrderListDTO> pageList=this.pageOrderListDTO(orderListBO.getPageSize(),orderListBO.getPageNo(),orderListDTOList);
        result.setOrderListDTOList(pageList);
        return result;
    }

    /**
     * @param pageSize
     * @param pageNo
     * @param orderListDTOList
     * @return
     * @Description 订单列表分页
     * @author lizengqiang
     * @date 2018/6/19 13:44
     */
    private List<OrderListDTO> pageOrderListDTO(Integer pageSize, Integer pageNo, List<OrderListDTO> orderListDTOList) {
        if (CollectionUtils.isEmpty(orderListDTOList)) {
            return Lists.newArrayList();
        }
        List<List<OrderListDTO>> groupList = Lists.partition(orderListDTOList, pageSize);
        if (pageNo == 0) {
            pageNo = 1;
        }
        if (pageNo > groupList.size()) {
            pageNo = groupList.size();
        }
        return groupList.get(pageNo - 1);
    }
    
    private OrderListDTO getOrderListDTO(OrderInfoListBO orderInfoListBO) {
        OrderListDTO orderListDTO = new OrderListDTO();
        orderListDTO.setBusinessCode(orderInfoListBO.getBusinessCode());
        orderListDTO.setSendTime(DateTimeUtil.format(orderInfoListBO.getSendTime(), DateTimeUtil.TimeFormat.LONG_DATE_PATTERN_SLASH));
        orderListDTO.setTransactionAmount(orderInfoListBO.getTransactionAmount() == null ? "0" : orderInfoListBO.getTransactionAmount().toString());
        orderListDTO.setTransactionCharge(orderInfoListBO.getTransactionCharge() == null ? "0" : orderInfoListBO.getTransactionCharge().toString());
        orderListDTO.setBankNumber(orderInfoListBO.getBankNumber());
        orderListDTO.setBankName(orderInfoListBO.getBankName());
        orderListDTO.setBankIcon(orderInfoListBO.getBankIcon());
        orderListDTO.setOrderStatus(orderInfoListBO.getOrderStatus());
        orderListDTO.setTransactionDate(DateTimeUtil.format(orderInfoListBO.getTransactionDate(), DateTimeUtil.TimeFormat.LONG_DATE_PATTERN_SLASH));
        orderListDTO.setExpectPricedDate(DateTimeUtil.format(orderInfoListBO.getExpectPricedDate(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
//        orderListDTO.setCompletedDate(DateTimeUtil.format(orderInfoListBO.getCompletedDate(), DateTimeUtil.TimeFormat.LONG_DATE_PATTERN_SLASH));
        orderListDTO.setRebalanceRate(orderInfoListBO.getRebalanceRate() == null ? "0" : orderInfoListBO.getRebalanceRate().toString());
        orderListDTO.setTransactionUnit(orderInfoListBO.getTransactionUnit() == null ? "0" : orderInfoListBO.getTransactionUnit().toString());
        orderListDTO.setFundName(StringUtils.defaultString(orderInfoListBO.getFundName()));

        orderListDTO.setActualTransactionAmount(orderInfoListBO.getActualTransactionAmount() == null || orderInfoListBO.getActualTransactionAmount().compareTo(orderInfoListBO.getTransactionAmount()) == 0 ? "0" : orderInfoListBO.getActualTransactionAmount().toString());
        orderListDTO.setPricedDate(DateTimeUtil.format(orderInfoListBO.getPricedDate(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
        orderListDTO.setCompletedDate(DateTimeUtil.format(orderInfoListBO.getCompletedDate(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
        return orderListDTO;
    }

    @Override
    public BigDecimal calculateFeeRate(FundMainModelDetailRequest fundMainModelDetailRequest, List<OrderFeeRateBO> orderFeeList) {
        BigDecimal result = BigDecimal.ZERO;
        APIResponse<List<FundMainModelDetailDTO>> fundDetailListResponse = fundMainModelRemote.queryFundMainModelDetailByCondition(fundMainModelDetailRequest);
        if (!fundDetailListResponse.isSuccess()) {
            return result;
        }
        List<FundMainModelDetailDTO> fundDetailList = fundDetailListResponse.getData();
        if (CollectionUtils.isEmpty(fundDetailList)) {
            return result;
        }
        if (CollectionUtils.isEmpty(orderFeeList)) {
            return result;
        }
        for (FundMainModelDetailDTO fundMainModelDetailDTO : fundDetailList) {
            for (OrderFeeRateBO orderFeeRateBO : orderFeeList) {
                if (Objects.equals(fundMainModelDetailDTO.getFundCode(), orderFeeRateBO.getFundCode())) {
                    // 基金占比
                    BigDecimal holdAmountScale = fundMainModelDetailDTO.getHoldAmountScale();
                    result = result.add(holdAmountScale.multiply(new BigDecimal("0.01")).multiply(orderFeeRateBO.getFeeRate()));
                }
            }
        }
        return result.setScale(2, BigDecimal.ROUND_DOWN);
    }

    @Override
    public OrderInfoDO queryByOrderNo(Long orderNo) {
        return orderInfoMapper.queryByOrderNo(orderNo);
    }
}
