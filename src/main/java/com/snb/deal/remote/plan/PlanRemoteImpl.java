package com.snb.deal.remote.plan;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.common.util.Model2DtoUtil;
import com.snb.deal.api.dto.plan.*;
import com.snb.deal.api.remote.plan.PlanRemote;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.bo.plan.PlanIncomeBO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.entity.plan.PlanPortfolioAccountDO;
import com.snb.deal.entity.plan.PlanPortfolioModel;
import com.snb.deal.enums.ResultCode;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.plan.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 计划相关dubbo服务
 */
@Slf4j
@Service(version = "1.0")
public class PlanRemoteImpl implements PlanRemote {

    @Resource
    private PlanBiz planBiz;
    @Resource
    private PlanService planService;
    @Resource
    private OrderInvestService orderInvestService;

    @Override
    public APIResponse<PlanResponse> createPlan(CreatePlanRequest request) {
        return planBiz.createPlan(request);
    }

    /**
     * 修改计划
     *
     * @param request
     * @return
     */
    @Override
    public APIResponse<PlanResponse> modifyPlan(ModifyPlanRequest request) {

        //1. 校验参数

        return planBiz.modifyPlan(request);
    }

    /**
     * 暂停计划
     *
     * @return
     */
    @Override
    public APIResponse<PlanResponse> suspendPlan(SuspendPlanRequest request) {

        //1. 校验参数

        return planBiz.suspendPlan(request);
    }

    @Override
    public APIResponse<PlanResponse> restartPlan(RestartPlanRequest request) {

        //1. 校验参数

        return planBiz.restartPlan(request);
    }

    /**
     * 计划详情，返回的数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanDetailResponse> planDetail(PlanDetailRequest request) {
        log.info("定投计划详情，request：{}", request);
        PlanDetailResponse planDetailResponse = new PlanDetailResponse();

        //0. 参数校验
        if (request.getPlanInfoId() == null) {
            log.error("定投计划详情参数校验失败！计划id为空！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 查询每月定投金额、定投日期、储蓄时间、开始时间、下次执行时间
        PlanInfoDO planInfo;
        try {
            planInfo = planService.getPlanInfo(request);
        } catch (Exception e) {
            log.error("查询定投计划详情异常！请求参数：{}", JSON.toJSONString(request), e);
            return APIResponse.build(ResultCode.PLAN_INFO_ERROR);
        }

        //2. 每月定投金额、定投日期、储蓄时间、开始时间、计划id、计划状态、下次定投日期
        if (planInfo != null) {
            planDetailResponse.setPlanId(planInfo.getPlanInfoId());
            planDetailResponse.setPlanStatus(planInfo.getPlanStatus());
            planDetailResponse.setPortfolioAmount(planInfo.getPortfolioAmount());
            planDetailResponse.setCycle(planInfo.getCycle());
            planDetailResponse.setCycleDay(planInfo.getCycleDay());
            planDetailResponse.setPortfolioYear(planInfo.getPortfolioYear());
            planDetailResponse.setCreateTime(planInfo.getCreateTime());
            planDetailResponse.setNextRunDate(planInfo.getNextRunDate());
        } else {
            log.info("没有查询到计划信息！请求参数：{}", JSON.toJSONString(request));
        }

        return APIResponse.build(SystemResultCode.SUCCESS).setData(planDetailResponse);
    }

    /**
     * 根据计划id查询计划信息，返回的数据可能为空
     *
     * @param planId
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanInfoResponse> getPlanInfo(Long planId) {
        log.info("获取计划信息，计划id：{}", planId);
        PlanInfoResponse planInfoResponse = new PlanInfoResponse();

        //0. 参数校验
        if (planId == null) {
            log.error("获取计划信息参数校验失败，计划id为空！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        //1. 查询计划信息
        PlanInfoDO planInfo;

        try {
            planInfo = planService.getPlanInfo(planId);
        } catch (Exception e) {
            log.error("查询计划信息异常！计划id：{}", planId, e);
            return APIResponse.build(ResultCode.PLAN_INFO_ERROR);
        }

        // 组织返回数据
        if (planInfo != null) {
            BeanUtils.copyProperties(planInfo, planInfoResponse);
        } else {
            log.info("查询到计划信息为空！计划id：{}", planId);
        }

        return APIResponse.build(SystemResultCode.SUCCESS).setData(planInfoResponse);
    }

    /**
     * 根据用户id查询计划列表
     *
     * @param userId 用户id
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<List<PlanInfoResponse>> getPlanInfoListByUserId(String userId) {
        log.info("获取用户id查询计划列表，userId：{}", userId);
        //0. 校验参数
        if (StringUtils.isEmpty(userId)) {
            log.error("查询计划列表参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 根据用户id查询计划列表
        List<PlanInfoDO> planInfoDOList;
        try {
            planInfoDOList = planService.listPlanInfo(userId);
        } catch (Exception e) {
            log.error("查询计划列表异常！用户：{}", userId, e);
            return APIResponse.build(ResultCode.PLAN_INFO_LIST_ERROR);
        }

        if (CollectionUtils.isEmpty(planInfoDOList)) {
            log.info("查询到计划列表为空！用户：{}", userId);
        }

        List<PlanInfoResponse> planInfoResponseList = Model2DtoUtil.model2Dto(planInfoDOList, PlanInfoResponse.class);

        //2. 组织返回数据
        return APIResponse.build(SystemResultCode.SUCCESS).setData(planInfoResponseList);
    }

    /**
     * 获取计划可赎回资产、确认中资产
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanAssetInfoResponse> getPlanAssetInfo(PlanAssetInfoRequest request) {
        log.info("获取计划可赎回资产、确认中资产，request：{}", JSON.toJSONString(request));
        //0. 校验参数
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getPlanId() == null) {
            log.error("获取计划可赎回资产、确认中资产参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 获取计划可赎回资产、确认中资产
        PlanAssetInfoResponse planAssetInfoResponse;
        try {
            planAssetInfoResponse = planBiz.getPlanAssetInfo(request.getUserId(), request.getPlanId(), request.getChannel());
        } catch (Exception e) {
            log.error("查询计划可赎回资产、确认中资产异常！用户：{}，计划：{}", request.getUserId(), request.getPlanId(), e);
            return APIResponse.build(ResultCode.PLAN_ASSET_INFO_ERROR);
        }

        if (planAssetInfoResponse == null) {
            log.info("查询到计划可赎回资产、确认中资产为空！request:{}", JSON.toJSONString(request));
        }

        //2. 组织返回数据
        return APIResponse.build(SystemResultCode.SUCCESS).setData(planAssetInfoResponse);
    }

    /**
     * 获取计划收益信息
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanIncomeInfoResponse> getPlanIncomeInfo(PlanIncomeInfoRequest request) {
        log.info("获取计划收益信息，request：{}", JSON.toJSONString(request));
        //0. 校验参数
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getPlanId() == null) {
            log.error("获取计划收益信息参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 获取计划累计收益
        PlanIncomeBO planIncomeInfo;
        try {
            planIncomeInfo = planBiz.getPlanIncomeInfo(request.getUserId(), request.getPlanId());
        } catch (Exception e) {
            log.error("查询计划累计收益信息异常！用户：{}，计划：{}", request.getUserId(), request.getPlanId(), e);
            return APIResponse.build(ResultCode.PLAN_ASSET_INFO_ERROR);
        }

        //2. 组织数据
        PlanIncomeInfoResponse planIncomeInfoResponse = new PlanIncomeInfoResponse();
        //BeanUtils.copyProperties(planIncomeInfo, planIncomeInfoResponse);
        planIncomeInfoResponse.setPlanTotalIncome(planIncomeInfo.getTotalIncome());

        //2. 组织返回数据
        return APIResponse.build(SystemResultCode.SUCCESS).setData(planIncomeInfoResponse);
    }

    /**
     * 获取计划可赎回、计划待确认资产详情
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanAssetDetailResponse> getPlanAssetDetail(PlanAssetDetailRequest request) {
        log.info("获取计划可赎回资产详情，request：{}", JSON.toJSONString(request));
        //0. 校验参数
        if (request == null || StringUtils.isEmpty(request.getUserId()) || request.getPlanId() == null) {
            log.error("获取计划可赎回资产详情参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 获取计划可赎回、计划待确认资产详情
        PlanAssetDetailResponse planAssetDetailResponse;
        try {
            planAssetDetailResponse = planBiz.getPlanAssetDetail(request.getUserId(), request.getPlanId());
        } catch (Exception e) {
            log.error("查询计划可赎回、计划待确认资产详情异常！用户：{}，计划：{}", request.getUserId(), request.getPlanId(), e);
            return APIResponse.build(ResultCode.PLAN_ASSET_DETAIL_INFO_ERROR);
        }

        if (planAssetDetailResponse == null) {
            log.info("查询到计划可赎回、计划待确认资产详情为空！request:{}", JSON.toJSONString(request));
        }

        //2. 组织返回数据
        return APIResponse.build(SystemResultCode.SUCCESS).setData(planAssetDetailResponse);
    }

    /**
     * 根据用户id和计划id查询计划持仓关系列表,返回的数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanPortfolioRelResponse> getPlanPortfolioRelList(PlanPortfolioRelRequest request) {
        log.info("获取计划持仓关系列表，request：{}", request);
        PlanPortfolioRelResponse planPortfolioRelResponse = new PlanPortfolioRelResponse();
        //0. 参数校验
        /*if (request.getPlanInfoId() == null) {
            log.error("获取计划持仓关系列表参数校验失败！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }*/

        //1. 根据用户id和计划id查询持仓关系列表
        List<PlanPortfolioModel> planPortfolioRelList = null;
        try {
            planPortfolioRelList = planService.queryUserPlanPortfolioModel(request.getUserId());
        } catch (Exception e) {
            log.error("查询计划持仓关系列表异常！用户：{}，计划id：{}", request.getUserId(), e);
            return APIResponse.build(ResultCode.PLAN_PORTFOLIO_REL_LIST_ERROR);
        }

        //2. 组织数据
        if (CollectionUtils.isNotEmpty(planPortfolioRelList)) {
            List<PlanPortfolioRelDTO> planPortfolioRelDTOList = Model2DtoUtil.model2Dto(planPortfolioRelList, PlanPortfolioRelDTO.class);
            planPortfolioRelResponse.setPlanPortfolioRelDTOList(planPortfolioRelDTOList);
        } else {
            log.info("查询到计划持仓关系列表为空！request:{}", JSON.toJSONString(request));
        }

        return APIResponse.build(SystemResultCode.SUCCESS).setData(planPortfolioRelResponse);
    }

    /**
     * 获取计划持仓账户信息（查询累计收益、在途资产、可赎回资产），返回的数据可能为空
     *
     * @param planId 计划id
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<PlanPortfolioAccountResponse> getPlanPortfolioAccount(Long planId) {
        log.info("获取计划持仓账户信息，planId：{}", planId);
        //0. 参数校验
        if (planId == null) {
            log.error("获取计划持仓账户信息参数校验失败，计划id为空！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 查询下次定投日期、扣款金额
        PlanPortfolioAccountDO planPortfolioAccount;
        try {
            planPortfolioAccount = planService.getPlanPortfolioAccount(planId);
        } catch (Exception e) {
            log.error("查询计划持仓账户异常！计划id：{}", planId, e);
            return APIResponse.build(ResultCode.PLAN_PORTFOLIO_ACCOUNT_ERROR);
        }

        //2.组织返回数据
        if (planPortfolioAccount != null) {
//            PlanPortfolioAccountResponse planPortfolioAccountResponse = new PlanPortfolioAccountResponse();
//            planPortfolioAccountResponse.setPlanPortfolioRelId(planPortfolioAccount.getPlanPortfolioRelId());
//            planPortfolioAccountResponse.setRedeemableAmount(planPortfolioAccount.getAvailableAmount());
//            planPortfolioAccountResponse.setTotalAmount(planPortfolioAccount.getTotalAmount());
//            planPortfolioAccountResponse.setTotalIntransitAmount(planPortfolioAccount.getTotalIntransitAmount());
//            planPortfolioAccountResponse.setTotalInvestAmount(planPortfolioAccount.getTotalInvestAmount());
//            planPortfolioAccountResponse.setUserId(planPortfolioAccount.getUserId());
//            planPortfolioAccountResponse.setPlanPortfolioAccountId(planPortfolioAccount.getPlanPortfolioAccountId());
//            planPortfolioAccountResponse.setTotalIncome(planPortfolioAccount.getTotalIncome());
        } else {
            log.info("查询到计划持仓账户为空！计划id：{}", planId);
        }

        return APIResponse.build(SystemResultCode.SUCCESS).setData(planPortfolioAccount);
    }

    /**
     * 获取用户计划定投记录
     *
     * @param request
     * @return
     * @author zhangwenting
     */
    @Override
    public APIResponse<PlanAutoInvestResponse> getPlanAutoInvestInfo( PlanAutoInvestRequest request){
        log.info("获取用户计划定投信息，userId:{}",request.getUserId());

        // 0.参数校验
        if (request.getUserId() == null) {
            log.error("获取计划定投管理信息参数校验失败，用户id为空！");
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        // 1. 根据用户id查找定投记录详情
        PlanAutoInvestResponse planAutoInvestResponse ;
        try {
            planAutoInvestResponse = planBiz.getPlanAutoInvestInfo(request);
        }catch (Exception e){
            log.error("查询用户定投计划定投记录异常！用户id：{}",request.getUserId() , e);
            return APIResponse.build(ResultCode.PLAN_INFO_LIST_ERROR);
        }
        if (planAutoInvestResponse == null) {
            log.info("查询到的用户定投计划定投记录详情为空！用户id：{}", request.getUserId());
        }

        //2. 组织返回数据
        return APIResponse.build(SystemResultCode.SUCCESS).setData(planAutoInvestResponse);
    }


}