package com.snb.deal.remote.order;

import com.alibaba.dubbo.config.annotation.Service;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.api.dto.redeem.*;
import com.snb.deal.api.remote.order.OrderRedeemRemote;
import com.snb.deal.biz.redeem.OrderRedeemBiz;
import com.snb.deal.bo.order.OrderRedeemBO;
import com.snb.deal.entity.plan.PlanPortfolioModel;
import com.snb.deal.enums.OrderStatusEnum;
import com.snb.deal.enums.ResultCode;
import com.snb.deal.service.order.OrderRedeemService;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service(version = "1.0")
public class OrderRedeemRemoteImpl implements OrderRedeemRemote {

    @Resource
    private OrderRedeemBiz orderRedeemBiz;

    @Resource
    private PlanService planService;

    @Resource
    private OrderRedeemService orderRedeemService;


    @Override
    public APIResponse<OrderRedeemResponse> orderRedeem(OrderRedeemRequest orderRedeemRequest) {
        //1.参数校验
        if (StringUtils.isEmpty(orderRedeemRequest.getUserId())
                || orderRedeemRequest.getTransactionAmount() == null
                || orderRedeemRequest.getInvestorPayId() == null
                || orderRedeemRequest.getInvestorPayId().intValue() == 0
                || StringUtils.isEmpty(orderRedeemRequest.getAccountNumber())) {
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        //2.获取计划持仓模型
        PlanPortfolioModel planPortfolioModel = this.getPlanPortfolioModel(orderRedeemRequest.getUserId());
        if (planPortfolioModel == null) {
            return APIResponse.build(ResultCode.PORTFOLIO_ERROR);
        }
        //3.参数封装
        OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
        orderRedeemBO.setAccountNumber(orderRedeemRequest.getAccountNumber());
        orderRedeemBO.setPortfolioCode(planPortfolioModel.getThirdPortfolioCode());
        orderRedeemBO.setPortfolioId(planPortfolioModel.getThirdPortfolioId());
        orderRedeemBO.setTransactionAmount(orderRedeemRequest.getTransactionAmount());
        orderRedeemBO.setInvestorPayId(orderRedeemRequest.getInvestorPayId());
        orderRedeemBO.setUserId(orderRedeemRequest.getUserId());
        //4.赎回金额校验
        ResultCode resultCode = orderRedeemService.checkOrderRedeem(orderRedeemBO);
        if (resultCode != ResultCode.REDEEM_CHECK_SUCCESS) {
            return APIResponse.build(resultCode);
        }
        log.info("orderRedeem-orderRedeemBO:{}", orderRedeemBO.toString());
        OrderRedeemResponse orderRedeemResponse = null;
        try {
            //5.发起赎回
            orderRedeemResponse = orderRedeemBiz.orderRedeem(orderRedeemBO);
        } catch (Exception e) {
            log.error("orderRedeem is error,orderRedeemRequest:{}", orderRedeemRequest.toString(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
        //6.如果申请失败，赎回订单流程走完，返回发起赎回失败
        if (orderRedeemResponse.getOrderStatus().intValue() == OrderStatusEnum.APPLY_FAIL.getCode()) {
            log.info("orderRedeem apply fail,orderRedeemRequest:{}", orderRedeemRequest.toString());
            return APIResponse.build(ResultCode.REDEEM_APPLY_ERROR);
        }
        return APIResponse.build(SystemResultCode.SUCCESS, orderRedeemResponse);
    }

    @Override
    public APIResponse<OrderRedeemFeeResponse> orderRedeemFee(OrderRedeemFeeRequest orderRedeemFeeRequest) {
        //1.参数校验
        if (StringUtils.isEmpty(orderRedeemFeeRequest.getUserId())
                || orderRedeemFeeRequest.getTransactionAmount() == null
                || StringUtils.isEmpty(orderRedeemFeeRequest.getAccountNumber())) {
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        //2.获取计划持仓模型
        PlanPortfolioModel planPortfolioModel = this.getPlanPortfolioModel(orderRedeemFeeRequest.getUserId());
        if (planPortfolioModel == null) {
            return APIResponse.build(ResultCode.PORTFOLIO_ERROR);
        }
        try {
            //3.参数封装
            OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
            orderRedeemBO.setAccountNumber(orderRedeemFeeRequest.getAccountNumber());
            orderRedeemBO.setTransactionAmount(orderRedeemFeeRequest.getTransactionAmount());
            orderRedeemBO.setPortfolioId(planPortfolioModel.getThirdPortfolioId());
            orderRedeemBO.setPortfolioCode(planPortfolioModel.getThirdPortfolioCode());
            //4.计算赎回费用和费率
            OrderRedeemFeeResponse orderRedeemFeeResponse = orderRedeemService.orderRedeemFee(orderRedeemBO);
            if (orderRedeemFeeResponse == null) {
                return APIResponse.build(ResultCode.REDEEM_FEE_ERROR);
            }
            return APIResponse.build(SystemResultCode.SUCCESS, orderRedeemFeeResponse);
        } catch (Exception e) {
            log.error("orderRedeemFee is error:orderRedeemFeeRequest:{}", orderRedeemFeeRequest.toString(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public APIResponse<OrderRedeemAmountResponse> orderRedeemAmount(OrderRedeemAmountRequest orderRedeemAmountRequest) {
        //1.参数校验
        if (StringUtils.isEmpty(orderRedeemAmountRequest.getUserId())) {
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        //2.获取计划持仓模型
        PlanPortfolioModel planPortfolioModel = this.getPlanPortfolioModel(orderRedeemAmountRequest.getUserId());
        if (planPortfolioModel == null) {
            return APIResponse.build(ResultCode.PORTFOLIO_ERROR);
        }
        //3.参数封装
        OrderRedeemBO orderRedeemBO = new OrderRedeemBO();
        orderRedeemBO.setPortfolioId(planPortfolioModel.getThirdPortfolioId());
        orderRedeemBO.setAccountNumber(orderRedeemAmountRequest.getAccountNumber());
        //4.获取组合赎回金额相关信息
        OrderRedeemAmountResponse orderRedeemAmountResponse = orderRedeemService.getOrderRedeemAmount(orderRedeemBO);
        if (orderRedeemAmountResponse == null) {
            return APIResponse.build(ResultCode.REDEEM_CHECK_ERROR);
        }
        return APIResponse.build(SystemResultCode.SUCCESS, orderRedeemAmountResponse);
    }

    @Override
    public APIResponse<String> getExpectedConfirmedDate(String userId, Long planId) {
        //1.参数校验
        if (StringUtils.isEmpty(userId)) {
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        //2.获取计划持仓模型
        PlanPortfolioModel planPortfolioModel = this.getPlanPortfolioModel(userId);
        if (planPortfolioModel == null) {
            return APIResponse.build(ResultCode.PORTFOLIO_ERROR);
        }
        try {
            String expectedConfirmedDate = orderRedeemService.getExpectedConfirmedDate(planPortfolioModel.getMainModelId());
            if (!StringUtils.isNumeric(expectedConfirmedDate)) {
                return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
            }
            return APIResponse.build(SystemResultCode.SUCCESS, DateTimeUtil.format(new Date(Long.parseLong(expectedConfirmedDate)), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH));
        } catch (Exception e) {
            log.error("getExpectedConfirmedDate is error,userId:{}", userId);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * @param userId
     * @return
     * @Description 根据计划id和用户id获取计划持仓模型
     * @author lizengqiang
     * @date 2018/4/28 14:54
     */
    private PlanPortfolioModel getPlanPortfolioModel(String userId) {
        List<PlanPortfolioModel> planPortfolioModelList = null;
        try {
            planPortfolioModelList = planService.queryUserPlanPortfolioModel(userId);
        } catch (Exception e) {
            log.error("queryUserPlanPortfolioModel is error", e);
        }
        if (CollectionUtils.isEmpty(planPortfolioModelList)) {
            return null;
        }
        PlanPortfolioModel planPortfolioModel = planPortfolioModelList.get(0);
        if (StringUtils.isEmpty(planPortfolioModel.getThirdPortfolioCode())
                || StringUtils.isEmpty(planPortfolioModel.getThirdPortfolioId())
                || !StringUtils.isNumeric(planPortfolioModel.getThirdPortfolioId())) {
            return null;
        }
        return planPortfolioModel;
    }

}
