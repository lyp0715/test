package com.snb.deal.remote.rebalance;

import com.alibaba.dubbo.config.annotation.Service;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.api.dto.rebalance.*;
import com.snb.deal.api.remote.rebalance.RebalanceRemote;
import com.snb.deal.bo.rebalance.OrderRebalanceSummaryConditionBO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDetailDO;
import com.snb.deal.service.order.OrderRebalanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Service(version = "1.0")
@Slf4j
public class RebalanceRemoteImpl implements RebalanceRemote {

    @Resource
    OrderRebalanceService orderRebalanceService;


    @Override
    public APIResponse<RebalanceAuthResponse> getRebalanceAutnBtn(RebalanceAntuRequest request){
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getChannel() == null) {
            log.error("RebalanceRemoteImpl#getRebalanceAutnCount=>参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        RebalanceAuthResponse rebalanceResponse = null;
        try {
            rebalanceResponse = orderRebalanceService.getRebalanceAutnBtn(request.getUserId(),request.getChannel());
        } catch (Exception e) {
            log.error("RebalanceRemoteImpl#getRebalanceAutnCount=>获取调仓按钮显示状态异常,userId = {}",request.getUserId(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
        return APIResponse.build(SystemResultCode.SUCCESS).setData(rebalanceResponse);
    }

    @Override
    public APIResponse<RebalanceAuthResponse> doRebalanceAutn(RebalanceAntuRequest request) {
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getChannel() == null) {
            log.error("RebalanceRemoteImpl#doRebalanceAutn=>参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        RebalanceAuthResponse rebalanceResponse = orderRebalanceService.doRebalanceAutn(request.getUserId(),request.getChannel());
        return APIResponse.build(SystemResultCode.SUCCESS).setData(rebalanceResponse);
    }

    @Override
    public APIResponse<RebalanceSummaryDTO> getLastRebalanceSummaryDatail(RebalanceRequest request) {
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getChannel() == null) {
            log.error("RebalanceRemoteImpl#getLastRebalanceSummaryDatail=>参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        RebalanceSummaryDTO rebalanceSummaryDTO = new RebalanceSummaryDTO();
        OrderRebalanceSummaryConditionBO orderRebalanceSummaryConditionBO = new OrderRebalanceSummaryConditionBO();
        orderRebalanceSummaryConditionBO.setUserId(request.getUserId());
        orderRebalanceSummaryConditionBO.setChannel(request.getChannel());
        OrderRebalanceSummaryDO orderRebalanceSummaryDO = null;
        try {
            orderRebalanceSummaryDO = orderRebalanceService.getLastRebalanceSummaryByCondition(orderRebalanceSummaryConditionBO);
        } catch (Exception e) {
            log.error("RebalanceRemoteImpl#getLastRebalanceSummaryDatail=>获取最新的调仓摘要异常,userId = {}",request.getUserId(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
        if(orderRebalanceSummaryDO == null){
            log.error("RebalanceRemoteImpl#getLastRebalanceSummaryDatail=>查询调仓摘要结果为空！");
            return APIResponse.build(SystemResultCode.BIZ_ERROR);
        }

        rebalanceSummaryDTO.setTotalFee(orderRebalanceSummaryDO.getTotalFee());

//        orderRebalanceSummaryConditionBO.setOrderRebalanceSummaryId(orderRebalanceSummaryDO.getOrderRebalanceSummaryId());
//        List<OrderRebalanceSummaryDetailDO> orderRebalanceSummaryDetailDOList = null;
//        List<RebalanceSummaryDetailDTO> rebalanceSummaryDetailDTOList = rebalanceSummaryDTO.getRebalanceSummaryDetailDTOList();
//        try {
//            orderRebalanceSummaryDetailDOList = orderRebalanceService.getRebalanceSummaryDetailByCondition(orderRebalanceSummaryConditionBO);
//            if(orderRebalanceSummaryDetailDOList != null){
//                for(OrderRebalanceSummaryDetailDO orderRebalanceSummaryDetailDO : orderRebalanceSummaryDetailDOList){
//                    RebalanceSummaryDetailDTO rebalanceSummaryDetailDTO = new RebalanceSummaryDetailDTO();
//                    BeanUtils.copyProperties(rebalanceSummaryDetailDTO,orderRebalanceSummaryDetailDO);
//                    rebalanceSummaryDetailDTOList.add(rebalanceSummaryDetailDTO);
//                }
//            }
//        } catch (Exception e) {
//            log.error("RebalanceRemoteImpl#getLastRebalanceSummaryDatail=>获取最新的调仓摘要详情异常,userId = {}",request.getUserId(), e);
//            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
//        }
        return APIResponse.build(SystemResultCode.SUCCESS).setData(rebalanceSummaryDTO);
    }

    @Override
    public APIResponse<RebalanceConfirmResponse> confirmRebalance(RebalanceRequest request) {
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getChannel() == null) {
            log.error("RebalanceRemoteImpl#confirmRebalance=>参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        RebalanceConfirmResponse rebalanceConfirmResponse = null;
        try {
            rebalanceConfirmResponse = orderRebalanceService.confirmRebalance(request);
        } catch (Exception e) {
            log.error("RebalanceRemoteImpl#confirmRebalance=>确认调仓异常,userId = {}",request.getUserId(), e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
        return  APIResponse.build(SystemResultCode.SUCCESS).setData(rebalanceConfirmResponse);
    }

}
