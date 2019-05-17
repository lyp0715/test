package com.snb.deal.remote.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.order.OrderListRequest;
import com.snb.deal.api.dto.order.OrderListResponse;
import com.snb.deal.api.remote.order.OrderInfoRemote;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.service.order.OrderInfoService;
import com.snb.user.dto.fund.GetUserBankCardRequest;
import com.snb.user.dto.fund.GetUserBankCardResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;


@Slf4j
@Service(version = "1.0")
public class OrderInfoRemoteImpl implements OrderInfoRemote {

    @Resource
    private OrderInfoService orderInfoService;

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;

    @Override
    public APIResponse<OrderListResponse> orderList(OrderListRequest orderListRequest) {
        String bankNumber = StringUtils.EMPTY;
        String bankName = StringUtils.EMPTY;
        String bankIcon=StringUtils.EMPTY;
        try {
            GetUserBankCardRequest request = new GetUserBankCardRequest();
            request.setFundChannel(FundChannelEnum.YIFENG);
            request.setUserId(orderListRequest.getUserId());
            APIResponse<GetUserBankCardResponse> responseAPIResponse = fundUserRemote.getUserBankCard(request);
            if (responseAPIResponse.isSuccess() && responseAPIResponse.getData() != null) {
                GetUserBankCardResponse userBankCardDO = responseAPIResponse.getData();
                bankNumber = StringUtils.defaultString(userBankCardDO.getBankNumber());
                bankName = StringUtils.defaultString(userBankCardDO.getBankName());
                bankIcon= StringUtils.defaultString(userBankCardDO.getBankIcon());
            }
        } catch (Exception e) {
            log.error("getUserBankCard is error,orderListRequest:{}", orderListRequest.toString(), e);
        }
        try {
            OrderListBO orderListBO = new OrderListBO();
            orderListBO.setUserId(orderListRequest.getUserId());
            if(orderListRequest.getBusinessCode()==null||orderListRequest.getBusinessCode()==0){
                orderListBO.setBusinessCode(null);
            }else {
                orderListBO.setBusinessCode(orderListRequest.getBusinessCode());
            }
            orderListBO.setBankName(bankName);
            orderListBO.setBankNumber(bankNumber);
            if (bankNumber.length() > 3) {
                orderListBO.setBankNumber(bankNumber.substring(bankNumber.length() - 4, bankNumber.length()));
            }
            orderListBO.setBankIcon(bankIcon);
            orderListBO.setPageNo(orderListRequest.getPageNo());
            orderListBO.setPageSize(orderListRequest.getPageSize());
            OrderListResponse orderListResponse = orderInfoService.orderList(orderListBO);
            return APIResponse.build(SystemResultCode.SUCCESS, orderListResponse);
        } catch (Exception e) {
            log.info("orderList is error,orderListRequest:{}", orderListRequest.toString(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
    }
}
