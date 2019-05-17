package com.snb.deal.biz.dividend.impl;

import com.snb.common.datetime.DateTimeUtil;
import com.snb.deal.api.dto.order.OrderDividendSyncRequest;
import com.snb.deal.biz.dividend.DividendBiz;
import com.snb.deal.bo.order.OrderDividendBO;
import com.snb.deal.service.order.OrderDividendService;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.YfQueryOrderListRequest;
import com.snb.third.yifeng.dto.order.YfQueryOrderListResponse;
import com.snb.third.yifeng.enums.YfTransactionTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 红利再投订单服务
 * @author lizengqiang
 * @date 2018/5/25 14:18
 */
@Slf4j
@Service
public class DividendBizImpl implements DividendBiz {

    @Resource
    private FundPortfolioService fundPortfolioService;
    @Resource
    private OrderDividendService orderDividendService;

    @Override
    public void syncDividendOrder(OrderDividendSyncRequest request) {
        if (StringUtils.isEmpty(request.getUserId()) || StringUtils.isEmpty(request.getAccountNumber())) {
            log.info("同步红利再投订单,参数为空,userId:{},accountNumber:{}", request.getUserId(), request.getAccountNumber());
            return;
        }
        log.info("同步强制赎回订单,userId:{},accountNumber:{}", request.getUserId(), request.getAccountNumber());
        String date = DateTimeUtil.getCurrentDatetime(DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_LINE);
        YfQueryOrderListRequest yfQueryOrderListRequest = YfQueryOrderListRequest.builder()
                .accountNumber(request.getAccountNumber())
                .transactionTypes(YfTransactionTypeEnum.DIVIDEND.getType())
                .pricedDateStart(date)
                .pricedDateEnd(date)
                .build();

        log.info("同步用户：{}发起红利再投订单请求：{}", request.getUserId(), yfQueryOrderListRequest);
        BaseResponse<YfQueryOrderListResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfQueryOrderListResponse>) fundPortfolioService.querySpecialOrderList(yfQueryOrderListRequest);
        } catch (Exception e) {
            log.error("DividendBizImpl-querySpecialOrderList is error,userId:{},accountNumber:{}", request.getUserId(), request.getAccountNumber(), e);
        }
        if (baseResponse == null || !baseResponse.success() || baseResponse.getData() == null || CollectionUtils.isEmpty(baseResponse.getData().getData())) {
            log.info("同步用户：{}发起红利再投查询结果为空：{}", request.getUserId(), yfQueryOrderListRequest);
            return;
        }
        log.info("同步用户：{}发起红利再投订单响应：{}", request.getUserId(), baseResponse.toString());
        try {
            OrderDividendBO orderDividendBO = new OrderDividendBO();
            orderDividendBO.setUserId(request.getUserId());
            orderDividendBO.setAccountNumber(request.getAccountNumber());
            orderDividendBO.setChannel(request.getChannel().getChannel());
            orderDividendService.createDividendOrder(orderDividendBO, baseResponse.getData());
        } catch (Exception e) {
            log.error("同步用户：{}红利再投订单保存异常", request.getUserId(), e);
        }
    }
}
