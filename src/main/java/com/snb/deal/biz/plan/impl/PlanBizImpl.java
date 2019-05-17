package com.snb.deal.biz.plan.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jianlc.event.EventMessageContext;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AccountSyncMessage;
import com.snb.common.mq.bean.PortfolioIncome;
import com.snb.common.mq.bean.PortfolioIncomeMessage;
import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.api.dto.plan.*;
import com.snb.deal.api.enums.plan.PlanInfoStatusEnum;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.bo.plan.PlanIncomeBO;
import com.snb.deal.bo.plan.PortfolioAccountBO;
import com.snb.deal.bo.plan.PortfolioAccountDetailBO;
import com.snb.deal.entity.incomesync.FundIncomeSyncDO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.plan.PlanChangeRecordDO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.entity.plan.PlanPortfolioAccountDO;
import com.snb.deal.entity.plan.PlanPortfolioRelDO;
import com.snb.deal.enums.ResultCode;
import com.snb.deal.mapper.plan.PlanChangeRecordMapper;
import com.snb.deal.mapper.plan.PlanPortfolioRelMapper;
import com.snb.deal.service.insurance.InsuranceService;
import com.snb.deal.service.order.OrderInfoService;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.plan.PlanService;
import com.snb.fund.api.dto.mainmodel.FundMainModelDTO;
import com.snb.fund.api.dto.mainmodel.FundMainModelRequest;
import com.snb.fund.api.enums.FundMainModelStatusEnums;
import com.snb.fund.api.remote.FundMainModelRemote;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.api.plan.FundPlanService;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeData;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeRequest;
import com.snb.third.yifeng.dto.income.YfPortfolioIncomeResponse;
import com.snb.third.yifeng.dto.plan.*;
import com.snb.third.yifeng.enums.plan.YfPlanCycleEnum;
import com.snb.user.dto.fund.*;
import com.snb.user.exception.BusinessException;
import com.snb.user.remote.FundUserRemote;
import com.snb.user.remote.UserAssetAndIncomeRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PlanBizImpl implements PlanBiz {

    @Resource
    PlanService planService;
    @Resource
    FundPortfolioService fundPortfolioService;
    @Resource
    FundPlanService fundPlanService;
    @Resource
    private InsuranceService insuranceService;
    @Reference(version = "1.0")
    FundMainModelRemote fundMainModelRemote;
    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Reference(version = "1.0")
    private UserAssetAndIncomeRemote userAssetAndIncomeRemote;
    @Resource
    private Environment environment;
    @Resource
    GuidCreater guidCreater;
    @Resource
    PlanChangeRecordMapper planChangeRecordMapper;
    @Autowired
    PlanPortfolioRelMapper planPortfolioRelMapper;
    @Resource
    OrderInvestService orderInvestService;
    @Resource
    OrderInfoService orderInfoService;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public APIResponse<PlanResponse> createPlan(CreatePlanRequest request) {

        log.info("创建定投计划，request：{}", request);
        PlanResponse createPlanResponse = new PlanResponse();

        /*//1. 查询主理人模型
        FundMainModelRequest fundMainModelRequest = new FundMainModelRequest();
        fundMainModelRequest.setFundMainModelStatusEnums(FundMainModelStatusEnums.EFFECTIVE);
        APIResponse<FundMainModelDTO> fundMainModelDTOAPIResponse = fundMainModelRemote.queryEffectiveFundMainModel(fundMainModelRequest);

        if (!fundMainModelDTOAPIResponse.isSuccess() || Objects.isNull(fundMainModelDTOAPIResponse.getData())) {
            return APIResponse.build(ResultCode.PLAN_CREATE_NO_FIND_MAINMODEL);
        }

        FundMainModelDTO fundMainModelDTO = fundMainModelDTOAPIResponse.getData();

        //2. 保存定投计划配置和计划模型关系
        PlanInfoDO planInfo = planService.savePlanInfo(request);

        //3. 奕丰接口调用
        YfCreatePlanRequest yfCreatePlanRequest = new YfCreatePlanRequest();

        yfCreatePlanRequest.setCycle(YfPlanCycleEnum.getPlanCycle(request.getCycle().getType()).getYfCycle());
        yfCreatePlanRequest.setCycleDay(request.getCycleDay());
        yfCreatePlanRequest.setPortfolioCode(fundMainModelDTO.getThirdPortfolioCode());
        yfCreatePlanRequest.setRspName(request.getPlanName());
        yfCreatePlanRequest.setRspPortfolioAmount(request.getPortfolioAmount());
        yfCreatePlanRequest.setAccountNumber(request.getFundUserAccount());
        yfCreatePlanRequest.setInvestorPayId(Integer.valueOf(request.getInvestorPayId()));

        String notifyUrl = environment.getProperty("snb.api.url.domain")+"/"+environment.getProperty("ifast.plan.callback.url");

        yfCreatePlanRequest.setNotifyUrl(notifyUrl);

        log.info("开始创建定投计划：{}", yfCreatePlanRequest);

        BaseResponse<YfCreatePlanResponse> baseResponse = null;

        try {
            baseResponse = (BaseResponse<YfCreatePlanResponse>) fundPlanService.createPortfolioPlan(yfCreatePlanRequest);
        } catch (Exception e) {
            log.error("创建定投计划异常", e);
        }

        log.info("用户：{}创建定投计划完成响应：{}", request.getUserId(), baseResponse);

        if (Objects.isNull(baseResponse)) {
            //无响应
            return APIResponse.build(ResultCode.PLAN_NO_RESPONSE);
        }

        if (!baseResponse.success()) {
            //创建失败
            log.error("用户：{}定投计划创建失败：{}", request.getUserId(), baseResponse.getCode() + ":" + baseResponse.getMessage());
            return APIResponse.build(ResultCode.PLAN_CREATE_FAILED);
        }

        YfCreatePlanResponse yfCreatePlanResponse = baseResponse.getData();

        //获取响应数据
        //第三方计划id
        String thirdPlanId = String.valueOf(yfCreatePlanResponse.getRspId());
        createPlanResponse.setThirdPlanId(thirdPlanId);
        //下次执行日期
        createPlanResponse.setNextRunDate(new DateTime(Long.valueOf(yfCreatePlanResponse.getNextRunDate())).toDate());

        planInfo.setThirdPlanId(thirdPlanId);
        planInfo.setNextRunDate(createPlanResponse.getNextRunDate());
        //持仓ID
        String portfolioId = String.valueOf(yfCreatePlanResponse.getPortfolioId());

        //4. 创建完成，保存计划任务，初始化计划统计信息，初始化用户持仓账户
        try {
            planService.afterPlanCreated(planInfo, fundMainModelDTO, portfolioId);
        } catch (Exception e) {
            log.error("创建计划完成，初始化数据异常", e);
            return APIResponse.build(ResultCode.PLAN_CREATE_INIT_FAILED);
        }

        return APIResponse.build(SystemResultCode.SUCCESS, createPlanResponse);*/

        String userId = request.getUserId();
        //根据id查询用户被系统暂停的计划
        PlanInfoDO planInfo = planService.queryUserSuspenPlan(userId);

        if (Objects.isNull(planInfo)) {
            log.error("创建计划失败，查询用户：{}暂停计划失败", userId);
            return APIResponse.build(ResultCode.PLAN_CREATE_INIT_FAILED);
        }

        log.info("定投计划创建修改前：{}", planInfo);

        //保存修改计划前定投信息
        PlanChangeRecordDO planChangeRecord = new PlanChangeRecordDO();
        planChangeRecord.setChannel(planInfo.getChannel());
        planChangeRecord.setCycle(planInfo.getCycle());
        planChangeRecord.setCycleDay(planInfo.getCycleDay());
        planChangeRecord.setPlanInfoId(planInfo.getPlanInfoId());
        planChangeRecord.setPlanName(planInfo.getPlanName());
        planChangeRecord.setPortfolioAmount(planInfo.getPortfolioAmount());
        planChangeRecord.setUserId(planInfo.getUserId());
        planChangeRecord.setThirdPlanId(planInfo.getThirdPlanId());
        planChangeRecord.setPlanchangeRecordId(guidCreater.getUniqueID());
        planChangeRecordMapper.insertChange(planChangeRecord);

        log.info("定投计划创建修改后：{}", request);

        YfModifyPlanRequest yfModifyPlanRequest = new YfModifyPlanRequest();
        yfModifyPlanRequest.setAccountNumber(request.getFundUserAccount());
        yfModifyPlanRequest.setRspId(Integer.valueOf(planInfo.getThirdPlanId()));
        yfModifyPlanRequest.setCycle(YfPlanCycleEnum.getPlanCycle(request.getCycle().getType()).getYfCycle());
        yfModifyPlanRequest.setCycleDay(request.getCycleDay());
        yfModifyPlanRequest.setRspPortfolioAmount(request.getPortfolioAmount());
        yfModifyPlanRequest.setRspName(planInfo.getPlanName());
        yfModifyPlanRequest.setInvestorPayId(Integer.valueOf(request.getInvestorPayId()));
        yfModifyPlanRequest.setRiskConfirmed(1);
        String notifyUrl = environment.getProperty("snb.api.url.domain")+"/"+environment.getProperty("ifast.plan.callback.url");

        yfModifyPlanRequest.setNotifyUrl(notifyUrl);
        // TODO notfiyURL

        log.info("开始创建修改定投计划：{}", yfModifyPlanRequest);

        BaseResponse<YfBasePlanResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.modifyPortfolioPlan(yfModifyPlanRequest);
        } catch (Exception e) {
            log.error("创建修改定投计划异常", e);
        }

        log.info("创建修改用户：{}定投计划完成响应：{}", userId, baseResponse);

        if (Objects.isNull(baseResponse)) {
            //无响应
            return APIResponse.build(ResultCode.PLAN_NO_RESPONSE);
        }

        if (!baseResponse.success() || Objects.isNull(baseResponse.getData())) {
            log.error("创建修改用户：{}定投计划失败：{}", userId, baseResponse.getCode() + ":" + baseResponse.getMessage());
            return APIResponse.build(ResultCode.PLAN_CREATE_FAILED);
        }

        YfBasePlanResponse yfBasePlanResponse = baseResponse.getData();

        //三方计划id
        createPlanResponse.setThirdPlanId(planInfo.getThirdPlanId());
        //下次执行日期
        createPlanResponse.setNextRunDate(new DateTime(Long.valueOf(yfBasePlanResponse.getNextRunDate())).toDate());
        //计划更新日期
        //planResponse.setLastUpdateTime(new DateTime(Long.valueOf(yfBasePlanResponse.getUpdatedDate())).toDate());

        planInfo.setNextRunDate(createPlanResponse.getNextRunDate());

        ModifyPlanRequest modifyPlanRequest = new ModifyPlanRequest();
        modifyPlanRequest.setCycleDay(request.getCycleDay());
        modifyPlanRequest.setCycle(request.getCycle());
        modifyPlanRequest.setPortfolioAmount(request.getPortfolioAmount());

        try {
            planService.afterPlanModified(planInfo, modifyPlanRequest);
        } catch (Exception e) {
            log.error("创建修改用户：{}计划信息异常", userId, e);
            return APIResponse.build(ResultCode.PLAN_CREATE_FAILED);
        }

        return APIResponse.build(SystemResultCode.SUCCESS, createPlanResponse);



    }

    @Override
    public APIResponse<PlanResponse> modifyPlan(ModifyPlanRequest request) {

//        APIResponse<PlanResponse> apiResponse = new APIResponse<>();

        PlanResponse planResponse = new PlanResponse();

        //根据id查询用户原计划信息
        PlanInfoDO planInfo = planService.queryPlanInfoById(request.getPlanId());

        if (Objects.isNull(planInfo)) {
            log.error("修改定投计划失败，查询用户：{}定投计划：{}失败", request.getUserId(), request.getPlanId());
            return APIResponse.build(ResultCode.PLAN_NO_PLAN);
        }

        //暂停的计划不能修改
        if (planInfo.getPlanStatus() == PlanInfoStatusEnum.SUSPEND.getStatus()) {
            log.error("修改定投计划，用户：{}定投计划：{}已暂停",request.getUserId(), request.getPlanId());
            return APIResponse.build(ResultCode.PLAN_ERROR_STATUS);
        }

        log.info("定投计划修改前：{}", planInfo);

        //保存修改计划前定投信息
        PlanChangeRecordDO planChangeRecord = new PlanChangeRecordDO();
        planChangeRecord.setChannel(planInfo.getChannel());
        planChangeRecord.setCycle(planInfo.getCycle());
        planChangeRecord.setCycleDay(planInfo.getCycleDay());
        planChangeRecord.setPlanInfoId(planInfo.getPlanInfoId());
        planChangeRecord.setPlanName(planInfo.getPlanName());
        planChangeRecord.setPortfolioAmount(planInfo.getPortfolioAmount());
        planChangeRecord.setUserId(planInfo.getUserId());
        planChangeRecord.setThirdPlanId(planInfo.getThirdPlanId());
        planChangeRecord.setPlanchangeRecordId(guidCreater.getUniqueID());
        planChangeRecordMapper.insertChange(planChangeRecord);

        log.info("定投计划修改后：{}", request);

        YfModifyPlanRequest yfModifyPlanRequest = new YfModifyPlanRequest();
        yfModifyPlanRequest.setAccountNumber(request.getFundUserAccount());
        yfModifyPlanRequest.setRspId(Integer.valueOf(planInfo.getThirdPlanId()));
        yfModifyPlanRequest.setCycle(YfPlanCycleEnum.getPlanCycle(request.getCycle().getType()).getYfCycle());
        yfModifyPlanRequest.setCycleDay(request.getCycleDay());
        yfModifyPlanRequest.setRspPortfolioAmount(request.getPortfolioAmount());
        yfModifyPlanRequest.setRspName(planInfo.getPlanName());
        yfModifyPlanRequest.setInvestorPayId(Integer.valueOf(request.getInvestorPayId()));
        yfModifyPlanRequest.setRiskConfirmed(1);
        String notifyUrl = environment.getProperty("snb.api.url.domain")+"/"+environment.getProperty("ifast.plan.callback.url");

        yfModifyPlanRequest.setNotifyUrl(notifyUrl);
        // TODO notfiyURL

        log.info("开始修改定投计划：{}", yfModifyPlanRequest);

        BaseResponse<YfBasePlanResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.modifyPortfolioPlan(yfModifyPlanRequest);
        } catch (Exception e) {
            log.error("修改定投计划异常", e);
        }

        log.info("修改用户：{}定投计划完成响应：{}", request.getUserId(), baseResponse);

        if (Objects.isNull(baseResponse)) {
            //无响应
            return APIResponse.build(ResultCode.PLAN_NO_RESPONSE);
        }

        if (!baseResponse.success() || Objects.isNull(baseResponse.getData())) {
            log.error("修改用户：{}定投计划失败：{}", request.getUserId(), baseResponse.getCode() + ":" + baseResponse.getMessage());
            return APIResponse.build(ResultCode.PLAN_MODIFY_REQ_FAILED);
        }

        YfBasePlanResponse yfBasePlanResponse = baseResponse.getData();

        //三方计划id
        planResponse.setThirdPlanId(planInfo.getThirdPlanId());
        //下次执行日期
        planResponse.setNextRunDate(new DateTime(Long.valueOf(yfBasePlanResponse.getNextRunDate())).toDate());
        //计划更新日期
        //planResponse.setLastUpdateTime(new DateTime(Long.valueOf(yfBasePlanResponse.getUpdatedDate())).toDate());

        planInfo.setNextRunDate(planResponse.getNextRunDate());

        try {
            planService.afterPlanModified(planInfo, request);
        } catch (Exception e) {
            log.error("修改用户：{}计划信息异常", request.getUserId(), e);
            return APIResponse.build(ResultCode.PLAN_MODIFY_FAILED);
        }

        return APIResponse.build(SystemResultCode.SUCCESS, planResponse);
    }

    @Override
    public APIResponse<PlanResponse> suspendPlan(SuspendPlanRequest request) {

//        APIResponse<PlanResponse> apiResponse = new APIResponse<>();

        PlanResponse planResponse = new PlanResponse();

        String userId = request.getUserId();
        Long planId = request.getPlanId();

        //查询用户计划信息
        PlanInfoDO planInfo = planService.queryUserPlanInfoById(request.getUserId(), request.getPlanId());

        if (Objects.isNull(planInfo)) {
            log.error("暂停计划，查询用户：{}，定投计划：{}失败", userId, planId);
            return APIResponse.build(ResultCode.PLAN_NO_PLAN);
        }

        //校验状态
        if (planInfo.getPlanStatus() != PlanInfoStatusEnum.ACTIVE.getStatus()) {
            log.error("暂停计划，定投计划：{}状态：{}异常", planId, planInfo.getPlanStatus());
            return APIResponse.build(ResultCode.PLAN_ERROR_STATUS);
        }

        //暂停计划
        YfBasePlanRequest yfBasePlanRequest = new YfBasePlanRequest();
        yfBasePlanRequest.setAccountNumber(request.getFundUserAccount());
        yfBasePlanRequest.setRspId(Integer.valueOf(planInfo.getThirdPlanId()));

        log.info("开始暂停用户：{}定投计划：{}", userId, yfBasePlanRequest);

        BaseResponse<YfBasePlanResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.suspendPortfolioPlan(yfBasePlanRequest);
        } catch (Exception e) {
            log.error("暂停定投计划异常", e);
        }

        log.info("暂停用户：{}定投计划完成响应：{}", userId, baseResponse);

        if (Objects.isNull(baseResponse)) {
            return APIResponse.build(ResultCode.PLAN_NO_RESPONSE);
        }

        if (!baseResponse.success() || Objects.isNull(baseResponse.getData())) {
            log.error("暂停用户：{}计划：{}失败：{}", userId, planId, baseResponse.getCode() + ":" + baseResponse.getMessage());
            return APIResponse.build(ResultCode.PLAN_SUSPEND_REQ_FAILED);
        }

        //获取响应数据
        YfBasePlanResponse yfBasePlanResponse = baseResponse.getData();

        //修改定投计划状态
        try {
            planService.afterPlanSuspended(planInfo);
        } catch (Exception e) {
            log.info("暂停用户：{}计划异常", planInfo.getPlanInfoId(), e);
            return APIResponse.build(ResultCode.PLAN_SUSPEND_FAILED);
        }

        planResponse.setThirdPlanId(planInfo.getThirdPlanId());
        planResponse.setLastUpdateTime(new DateTime(Long.valueOf(yfBasePlanResponse.getUpdatedDate())).toDate());

        return APIResponse.build(SystemResultCode.SUCCESS, planResponse);
    }

    @Override
    public APIResponse<PlanResponse> restartPlan(RestartPlanRequest request) {

//        APIResponse<PlanResponse> apiResponse = new APIResponse<>();

        PlanResponse planResponse = new PlanResponse();

        String userId = request.getUserId();
        Long planId = request.getPlanId();

        //查询用户计划信息
        PlanInfoDO planInfo = planService.queryUserPlanInfoById(userId, planId);

        if (Objects.isNull(planInfo)) {
            log.error("重启计划，查询用户：{}，定投计划：{}失败", userId, planId);
            return APIResponse.build(ResultCode.PLAN_NO_PLAN);
        }

        //校验状态
        if (planInfo.getPlanStatus() != PlanInfoStatusEnum.SUSPEND.getStatus()) {
            log.error("重启计划，定投计划：{}状态：{}异常", planId, planInfo.getPlanStatus());
            return APIResponse.build(ResultCode.PLAN_ERROR_STATUS);
        }

        //重启计划
        YfBasePlanRequest yfBasePlanRequest = new YfBasePlanRequest();
        yfBasePlanRequest.setAccountNumber(request.getFundUserAccount());
        yfBasePlanRequest.setRspId(Integer.valueOf(planInfo.getThirdPlanId()));

        log.info("重启用户：{}定投计划：{}", userId, yfBasePlanRequest);

        BaseResponse<YfBasePlanResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.restartPortfolioPlan(yfBasePlanRequest);
        } catch (Exception e) {
            log.error("重启定投计划异常", e);
        }

        log.info("重启用户：{}定投计划完成响应：{}", userId, baseResponse);

        if (Objects.isNull(baseResponse)) {
            return APIResponse.build(ResultCode.PLAN_NO_RESPONSE);
        }

        if (!baseResponse.success() || Objects.isNull(baseResponse.getData())) {
            return APIResponse.build(ResultCode.PLAN_RESTART_REQ_FAILED);
        }

        //获取响应数据
        YfBasePlanResponse yfBasePlanResponse = baseResponse.getData();

        planResponse.setThirdPlanId(planInfo.getThirdPlanId());
        planResponse.setLastUpdateTime(new DateTime(Long.valueOf(yfBasePlanResponse.getUpdatedDate())).toDate());
        planResponse.setNextRunDate(new DateTime(Long.valueOf(yfBasePlanResponse.getNextRunDate())).toDate());

        planInfo.setNextRunDate(planResponse.getNextRunDate());
        //修改定投计划状态
        try {
            planService.afterPlanRestarted(planInfo);
        } catch (Exception e) {
            log.info("重启用户：{}计划异常", planInfo.getPlanInfoId(), e);
            return APIResponse.build(ResultCode.PLAN_RESTART_FAILED);
        }

        return APIResponse.build(SystemResultCode.SUCCESS, planResponse);
    }

    //    @Event(reliability = true,
//            value = "transactionManager",
//            eventType = "'testSend'",
//            eventId = "#date.getTime()",
//            queue = "test.event.queue",
//            exchange = "",
//            amqpTemplate = "",
//            version = ""
//    )
    @Override
    public void syncPortfolioIncome(PlanPortfolioRelDO planPortfolioRelDO, String accountNumber, FundIncomeSyncDO fundIncomeSync) {

        List<PortfolioIncome> portfolioIncomeList = Lists.newArrayList();

        YfPortfolioIncomeRequest incomeRequest = YfPortfolioIncomeRequest.builder()
                .accountNumber(accountNumber)
                .portfolioId(Integer.valueOf(planPortfolioRelDO.getThirdPortfolioId()))
//                .showedDateStart(new DateTime(fundIncomeSync.getSyncStartDate()).toString("yyyy-MM-dd"))
//                .showedDateEnd(new DateTime(fundIncomeSync.getSyncEndDate()).toString("yyyy-MM-dd"))
                .build();

        BaseResponse<YfPortfolioIncomeResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfPortfolioIncomeResponse>) fundPlanService.queryPortfolioIncome(incomeRequest);
        } catch (Exception e) {
            log.error("同步用户：{} {}日收益列表异常", planPortfolioRelDO.getUserId(), fundIncomeSync.getSyncDate());
        }

        if (!Objects.isNull(baseResponse) && !Objects.isNull(baseResponse.getData())) {
            YfPortfolioIncomeResponse yfPortfolioIncomeResponse = baseResponse.getData();
            for (YfPortfolioIncomeData yfPortfolioIncomeData : yfPortfolioIncomeResponse.getData()) {
                PortfolioIncome portfolioIncome = new PortfolioIncome();
                portfolioIncome.setAccumulatedPerformance(yfPortfolioIncomeData.getAccumulatedPerformance());
                portfolioIncome.setAccumulatedProfitLoss(yfPortfolioIncomeData.getAccumulatedProfitLoss());
                portfolioIncome.setPerformanceDaily(yfPortfolioIncomeData.getPerformanceDaily());
                portfolioIncome.setProfitLossDaily(yfPortfolioIncomeData.getProfitLossDaily());
                portfolioIncome.setShowedDate(DateTime.parse(yfPortfolioIncomeData.getShowedDate()).toDate());
                portfolioIncome.setUpdatedDate(DateTime.parse(yfPortfolioIncomeData.getUpdatedDate()).toDate());

                portfolioIncomeList.add(portfolioIncome);
            }
        }

        PortfolioIncomeMessage portfolioIncomeMessage = new PortfolioIncomeMessage();
        portfolioIncomeMessage.setPortfolioIncomeList(portfolioIncomeList);
        portfolioIncomeMessage.setUserId(fundIncomeSync.getUserId());
        portfolioIncomeMessage.setPlanPortfolioRelId(fundIncomeSync.getPlanPortfolioRelId());

        EventMessageContext.addMessage(portfolioIncomeMessage);

    }

    @Override
    public void syncPlanAccount(PlanAccountSyncRequest request) {

        String userId = request.getUserId();
        Long planId = request.getPlanId();

        List<PlanPortfolioRelDO> planPortfolioRelDOS = planService.listPlanPortfolioRel(userId, planId, FundChannelEnum.YIFENG.getChannel());

        if (CollectionUtils.isEmpty(planPortfolioRelDOS)) {
            log.error("同步用户：{}计划：{}账户信息，查询用户计划持仓失败", userId, planId);
            return;
        }

        PlanPortfolioRelDO planPortfolioRelDO = planPortfolioRelDOS.get(0);
        if (StringUtils.isEmpty(planPortfolioRelDO.getThirdPortfolioId())) {
            log.error("同步用户：{}计划：{}账户信息，暂无持仓ID", userId, planId);
            return;
        }

        //查询用户持仓账户
        PlanPortfolioAccountDO planPortfolioAccountDO = planService.queryUserAccountByPlanPortfolioRelId(planPortfolioRelDO.getPlanPortfolioRelId());

        if (Objects.isNull(planPortfolioAccountDO)) {
            log.error("同步用户：{}计划：{}账户信息，查询用户持仓账户失败", userId, planId);
            return;
        }

        YfPortfolioAccountRequest yfPortfolioAccountRequest = new YfPortfolioAccountRequest();
        yfPortfolioAccountRequest.setAccountNumber(request.getFundUserAccount());
        yfPortfolioAccountRequest.setPortfolioId(Integer.valueOf(planPortfolioRelDO.getThirdPortfolioId()));

        BaseResponse<YfPortfolioAccountResponse> baseResponse = null;
        try {
            baseResponse = (BaseResponse<YfPortfolioAccountResponse>) fundPlanService.queryPortfolioAccount(yfPortfolioAccountRequest);
        } catch (Exception e) {
            log.error("同步用户：{}计划：{}账户信息，请求异常", userId, planId, e);
        }

        log.info("同步用户：{}计划：{}账户信息，响应完成：{}", userId, planId, baseResponse);

        if (Objects.isNull(baseResponse)) {
            log.error("同步用户：{}计划：{}账户信息，请求无响应", userId, planId);
            return;
        }

        if (!baseResponse.success() || Objects.isNull(baseResponse.getData())) {
            log.error("同步用户：{}计划：{}账户信息，请求失败：{}", userId, planId, baseResponse.getMessage());
            return;
        }

        YfPortfolioAccountResponse yfPortfolioAccountResponse = baseResponse.getData();

        if (CollectionUtils.isNotEmpty(yfPortfolioAccountResponse.getData())) {
            YfPortfolioAccount yfPortfolioAccount = yfPortfolioAccountResponse.getData().get(0);
            PlanPortfolioAccountDO accountDO = new PlanPortfolioAccountDO();
            //可用资产
            accountDO.setAvailableAmount(yfPortfolioAccount.getAvailableAmount());
            //总在途资产
            accountDO.setTotalIntransitAmount(yfPortfolioAccount.getIntransitAssetsTotal());
            //总资产
            accountDO.setTotalAmount(yfPortfolioAccount.getAvailableAmount().add(yfPortfolioAccount.getIntransitAssetsTotal()));

            BigDecimal totalInvestAmount = BigDecimal.ZERO;
            BigDecimal availableUnit = BigDecimal.ZERO;

            //持仓列表
            if (!Objects.isNull(yfPortfolioAccount.getFundHoldings())
                    && CollectionUtils.isNotEmpty(yfPortfolioAccount.getFundHoldings().getData())) {

                for (YfPortfolioHoldingData yfPortfolioHoldingData : yfPortfolioAccount.getFundHoldings().getData()) {
                    totalInvestAmount = totalInvestAmount.add(yfPortfolioHoldingData.getInvestmentAmount());
                    availableUnit = availableUnit.add(yfPortfolioHoldingData.getAvailableUnit());
                }

            }

            accountDO.setTotalAmount(totalInvestAmount);
            accountDO.setAvailableUnit(availableUnit);
            accountDO.setPlanPortfolioAccountId(planPortfolioAccountDO.getPlanPortfolioAccountId());

            //更新持仓账户
            planService.updatePlanPortfolioAccount(accountDO);

            log.info("用户：{}计划：{}账户同步完成：{}", userId, planId, accountDO);
        }

    }

    @Override
    public PlanAssetInfoResponse getPlanAssetInfo(String userId, Long planId, Integer channel) throws Exception {
        PlanAssetInfoResponse planAssetInfoResponse = new PlanAssetInfoResponse();

        PortfolioAccountBO portfolioAccountBO = getPortfolioAccountBO(userId, planId, channel);
        log.info("获取到持仓账户信息：[{}]，用户：[{}]，计划：[{}]", JSON.toJSONString(portfolioAccountBO), userId, planId);
        if (portfolioAccountBO == null) {
            return planAssetInfoResponse;
        }

        //计划可赎回资产
        planAssetInfoResponse.setPlanRedeemableAmount(portfolioAccountBO.getAvailableAmount());
        //计划在途资产
        planAssetInfoResponse.setPlanTotalInTransitAmount(portfolioAccountBO.getTotalIntransitAmount());

        return planAssetInfoResponse;
    }

    @Override
    public PlanAssetDetailResponse getPlanAssetDetail(String userId, Long planId) throws Exception {
        PlanAssetDetailResponse planAssetDetailResponse = new PlanAssetDetailResponse();
        //持有详情
        List<PlanFundDetailDTO> planRedeemableFundDTOList = new ArrayList<>();
        //确认中详情
        List<PlanFundDetailDTO> planConfirmingFundDTOList = new ArrayList<>();

        //初始化列表
        planAssetDetailResponse.setPlanConfirmingFundDTOList(planConfirmingFundDTOList);
        planAssetDetailResponse.setPlanRedeemableFundDTOList(planRedeemableFundDTOList);

        PortfolioAccountBO portfolioAccountBO = getPortfolioAccountBO(userId, planId, FundChannelEnum.YIFENG.getChannel());
        log.info("获取到持仓账户信息：[{}]，用户：[{}]，计划：[{}]", JSON.toJSONString(portfolioAccountBO), userId, planId);
        if (portfolioAccountBO == null) {
            return planAssetDetailResponse;
        }

        List<PortfolioAccountDetailBO> portfolioAccountDetailBOList = portfolioAccountBO.getPortfolioAccountDetailBOList();
        if (CollectionUtils.isEmpty(portfolioAccountDetailBOList)) {
            return planAssetDetailResponse;
        }

        for (PortfolioAccountDetailBO portfolioAccountDetailBO : portfolioAccountDetailBOList) {
            // 持有中资产
            BigDecimal availableUnit = portfolioAccountDetailBO.getAvailableUnit();
            BigDecimal nav = portfolioAccountDetailBO.getNav();
            BigDecimal holdingAmount = availableUnit.multiply(nav).setScale(2, RoundingMode.HALF_DOWN);
            if (holdingAmount.equals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))) {
                continue;
            }

            PlanFundDetailDTO planFundDetailDTO = new PlanFundDetailDTO();
            //累计收益
            planFundDetailDTO.setAccumulatedProfitloss(portfolioAccountDetailBO.getTotalIncome());
            //持有中资产
            planFundDetailDTO.setCurrentValue(holdingAmount);
            //基金代码
            planFundDetailDTO.setFundCode(portfolioAccountDetailBO.getFundCode());
            //基金名称
            planFundDetailDTO.setFundName(portfolioAccountDetailBO.getFundName());
            //基金类型
            planFundDetailDTO.setFundType(portfolioAccountDetailBO.getFundType());
            //占比
            planFundDetailDTO.setHoldAmountScale(portfolioAccountDetailBO.getProportion());
            //持有总份额
            planFundDetailDTO.setTotalUnit(portfolioAccountDetailBO.getTotalUnit());

            planRedeemableFundDTOList.add(planFundDetailDTO);
        }
        for (PortfolioAccountDetailBO portfolioAccountDetailBO : portfolioAccountDetailBOList) {
            // 在途资产 = 总资产 - 可用金额（可用份额 * 净值）
            // 2018年5月31日，基金1.2期需求，在途资产不包含未分配收益，这里还要减去未分配收益
            BigDecimal availableAmount = portfolioAccountDetailBO.getAvailableUnit().multiply(portfolioAccountDetailBO.getNav());
            BigDecimal totalIntransitAmount = portfolioAccountDetailBO.getCurrentValue().subtract(availableAmount).
                    subtract(portfolioAccountDetailBO.getUndistributedIncome()).setScale(2, RoundingMode.HALF_DOWN);
            if (totalIntransitAmount.equals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))) {
                continue;
            }

            PlanFundDetailDTO planFundDetailDTO = new PlanFundDetailDTO();
            //在途资产（确认中资产）
            planFundDetailDTO.setCurrentValue(totalIntransitAmount);
            //基金代码
            planFundDetailDTO.setFundCode(portfolioAccountDetailBO.getFundCode());
            //基金名称
            planFundDetailDTO.setFundName(portfolioAccountDetailBO.getFundName());
            //基金类型
            planFundDetailDTO.setFundType(portfolioAccountDetailBO.getFundType());

            planConfirmingFundDTOList.add(planFundDetailDTO);
        }

        return planAssetDetailResponse;
    }

    private PortfolioAccountBO getPortfolioAccountBO(String userId, Long planId, Integer channel) throws Exception {
        String userAccountWhitelist = environment.getProperty("whitelist.userAccount");
        log.info("从配置中心获取白名单账户为[{}]，用户：[{}]，计划：[{}]，渠道：[{}]", userAccountWhitelist, userId, planId, channel);
        // 对于不在白名单中的用户调用三方接口实时获取
        if (StringUtils.isEmpty(userAccountWhitelist)) {
            log.info("获取用户资产和基金信息=>从配置中心获取白名单账户为空，用户：[{}]", userId);
            return queryPortfolioAccountFromThird(userId, planId);
        }
        String[] userIds = userAccountWhitelist.split(",");
        if (!Arrays.asList(userIds).contains(userId)) {
            return queryPortfolioAccountFromThird(userId, planId);
        }
        return queryPortfolioAccountFromDB(userId, planId, channel);
    }

    /**
     * 从第三方实时查询持仓账户信息
     * @param userId
     * @param planId
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public PortfolioAccountBO queryPortfolioAccountFromThird(String userId, Long planId) throws Exception {
        log.info("从三方实时查询持仓账户信息，用户：[{}]，计划：[{}]", userId, planId);
        //1. 获取用户基金账户号
        GetUserFundAccountInfoResponse data = getUserFundAccountInfo(userId);
        String accountNumber = data.getAccountNumber();
        if (StringUtils.isEmpty(accountNumber)) {
            throw new RuntimeException("获取用户基金账户accountNumber为空！");
        }
        //2. 获取计划持仓组合代码
        String planPortfolioId = getPlanPortfolioId(userId, planId);

        if (StringUtils.isEmpty(accountNumber)) {
            throw new RuntimeException("获取用户基金账户accountNumber为空！");
        }
        //查询计划持仓账户
        YfPortfolioAccountRequest yfPortfolioAccountRequest = new YfPortfolioAccountRequest();
        yfPortfolioAccountRequest.setAccountNumber(accountNumber);
        if (StringUtils.isNotBlank(planPortfolioId)) {
            yfPortfolioAccountRequest.setPortfolioId(Integer.valueOf(planPortfolioId));
        }

        log.info("查询用户：{}计划：{}账户信息请求：{}",userId,planId,yfPortfolioAccountRequest);

        BaseResponse<YfPortfolioAccountResponse> baseResponse = (BaseResponse<YfPortfolioAccountResponse>) fundPlanService.queryPortfolioAccount(yfPortfolioAccountRequest);

        log.info("查询用户：{}计划：{}账户信息，响应完成：{}",userId,planId,baseResponse);

        Preconditions.checkNotNull(baseResponse,"查询用户:%s计划:%s持有账户信息无响应",userId,planId);

        Preconditions.checkState(baseResponse.success(),"查询用户:%s计划:%s持有账户信息失败:%s",userId,planId,baseResponse.getCode()+":"+baseResponse.getMessage());

        if (Objects.isNull(baseResponse.getData()) || CollectionUtils.isEmpty(baseResponse.getData().getData())) {
            log.info("查询用户:{}计划:{}持有账户信息无数据",userId,planId);
            return null;
        }

        return establishThirdPortfolioAccountData(userId, planId, baseResponse);
    }

    @Override
    public void modifyInvestorPayId(String userId, Integer investorPayId) throws Exception{

        //查询基金账户
        BaseFundRequest req = new BaseFundRequest();
        req.setUserId(userId);
        req.setFundPlatform(FundChannelEnum.YIFENG);
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);

        Preconditions.checkState(!Objects.isNull(fundUserResponse)&&fundUserResponse.isSuccess(),"查询用户%s基金账户失败",userId);

        String accountNumber = fundUserResponse.getData().getAccountNumber();

        //查询用户有效计划
        //因业务中暂无终止场景，此查询未考虑终止状态计划
        List<PlanInfoDO> planInfoDOList = planService.listPlanInfo(userId);

        if (CollectionUtils.isEmpty(planInfoDOList)) {
            log.info("修改用户:{}计划支付代码，无有效计划",userId);
            return;
        }

        //兼容多计划
        for (PlanInfoDO planInfoDO : planInfoDOList) {

            Long planId = planInfoDO.getPlanInfoId();

            YfModifyPlanRequest yfModifyPlanRequest = new YfModifyPlanRequest();
            yfModifyPlanRequest.setAccountNumber(accountNumber);
            yfModifyPlanRequest.setRspId(Integer.valueOf(planInfoDO.getThirdPlanId()));
            yfModifyPlanRequest.setCycle(YfPlanCycleEnum.getPlanCycle(planInfoDO.getCycle()).getYfCycle());
            yfModifyPlanRequest.setCycleDay(planInfoDO.getCycleDay());
            yfModifyPlanRequest.setRspPortfolioAmount(planInfoDO.getPortfolioAmount());
            yfModifyPlanRequest.setRspName(planInfoDO.getPlanName());
            yfModifyPlanRequest.setInvestorPayId(investorPayId);
            yfModifyPlanRequest.setRiskConfirmed(1);
            String notifyUrl = environment.getProperty("snb.api.url.domain")+"/"+environment.getProperty("ifast.plan.callback.url");

            yfModifyPlanRequest.setNotifyUrl(notifyUrl);
            // TODO notfiyURL

            log.info("开始修改定投计划支付代码：{}", yfModifyPlanRequest);

            BaseResponse<YfBasePlanResponse> baseResponse = null;
            try {
                baseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.modifyPortfolioPlan(yfModifyPlanRequest);
            } catch (Exception e) {
                log.error("修改用户:{}定投计划:{}支付代码异常",userId,planId, e);
                throw new Exception(e);
            }

            log.info("修改用户：{}定投计划:{}支付完成响应：{}", userId,planId, baseResponse);

            Preconditions.checkNotNull(baseResponse,"修改定投计划:%s支付代码无响应",planId);

            Preconditions.checkState(baseResponse.success()&&!Objects.isNull(baseResponse.getData()),"修改定投计划:%s支付代码失败message:%s",planId,baseResponse.getCode()+":"+baseResponse.getMessage());

        }
    }

    @Override
    public PlanAutoInvestResponse getPlanAutoInvestInfo(PlanAutoInvestRequest request) throws Exception {
        PlanAutoInvestResponse planAutoInvestResponse = new PlanAutoInvestResponse();
        List<PlanAutoInvestDetailDTO> planAutoInvestDetailDTOList = Lists.newArrayList();
        PageInfo<OrderInvestDO> page;
        //1.1 orderInvest表查询所有定投订单
        try{
            page = orderInvestService.pageOrderPlanAutoInvest(request);
        }catch(Exception e){
            log.error("获取用户定投订单信息异常！用户: {}", request.getUserId(), e);
            return planAutoInvestResponse;
        }

        //1.2 遍历每一个订单，根据order_no 和 order_invest_id查询定投详情
        if(page != null) {
            page.getList().forEach(orderInvestDO -> {
                PlanAutoInvestDetailDTO planAutoInvestDetailDTO = new PlanAutoInvestDetailDTO();
                //1.2.1 根据 order_no 在 order_info 表查找日期和金额
                OrderInfoDO orderInfoDO = orderInfoService.queryByOrderNo(orderInvestDO.getOrderNo());
                //1.2.2 根据 order_invest_id 在 order_invest_detail 表 计算实际金额和状态
                OrderInvestDetailRequest orderInvestDetailRequest = new OrderInvestDetailRequest();
                orderInvestDetailRequest.setOrderInvestId(orderInvestDO.getOrderInvestId());
                List<OrderInvestDetailDO> listOrderInvestDetail = orderInvestService.listOrderInvestDetail(orderInvestDetailRequest);
                BigDecimal transactionAmount = new BigDecimal(0);   //实际金额
                ArrayList<Integer> statusList = new ArrayList<Integer>();
                String  transactionStatus = null;  //状态
                int count = 0;
                for(OrderInvestDetailDO orderInvestDetailDO: listOrderInvestDetail ){
                    if(orderInvestDetailDO.getTransactionStatus() == 201 || orderInvestDetailDO.getTransactionStatus() == 301
                            || orderInvestDetailDO.getTransactionStatus() == 101){
                        transactionAmount = transactionAmount.add(orderInvestDetailDO.getTransactionAmount());
                        count++;
                    }
                    statusList.add(orderInvestDetailDO.getTransactionStatus());    //把状态放到一个list里
                }
                if(statusList.contains(501) || statusList.contains(401) || statusList.contains(200)){
                    transactionStatus = "处理中";
                }else if(statusList.contains(201) || statusList.contains(301) || statusList.contains(101) || count == listOrderInvestDetail.size()){
                    transactionStatus = "成功";
                }else{
                    transactionStatus = "失败";
                }
                planAutoInvestDetailDTO.setAmount(orderInfoDO.getTransactionAmount());
                String transactionDateString = DateTimeUtil.format(orderInfoDO.getCreateTime(), DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_SLASH);
                planAutoInvestDetailDTO.setTransactionDate(transactionDateString);
                planAutoInvestDetailDTO.setTransactionAmount(transactionAmount);
                planAutoInvestDetailDTO.setTransactionStatus(transactionStatus);
                planAutoInvestDetailDTOList.add(planAutoInvestDetailDTO);
            });
            planAutoInvestResponse.setTotalCount(page.getTotal());
            planAutoInvestResponse.setPlanAutoInvestInfoDTOList(planAutoInvestDetailDTOList);
            planAutoInvestResponse.setPageNo(request.getPageNo());
            planAutoInvestResponse.setPageSize(request.getPageSize());
        }
        return planAutoInvestResponse;
    }

    @Override
    public void initPlan(String userId) throws Exception {

        String key = "initPlan"+userId;
        if (!redisTemplate.getConnectionFactory().getConnection().setNX(key.getBytes(StandardCharsets.UTF_8),new byte[0])) {
            log.info("风险测评后，创建用户：{}定投计划，重复提交");
            return;
        }

        redisTemplate.expire(key,5, TimeUnit.SECONDS);

        //是否已经有有效的计划持仓关系
        List<PlanPortfolioRelDO> planPortfolioRelDOS = planPortfolioRelMapper.selectByUserIdAndChannel(userId,FundChannelEnum.YIFENG.getChannel());
        if (CollectionUtils.isNotEmpty(planPortfolioRelDOS)) {
            log.info("风险测评后，创建用户计划，用户：{}已经存在计划",userId);
            return;
        }

        //查询用户基金账户
        BaseFundRequest req = new BaseFundRequest();
        req.setUserId(userId);
        req.setFundPlatform(FundChannelEnum.YIFENG);
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);

        Preconditions.checkState(!Objects.isNull(fundUserResponse)&&fundUserResponse.isSuccess(),"查询用户%s基金账户失败",userId);

        GetUserFundAccountInfoResponse userFundAccount = fundUserResponse.getData();

        //查询主理人模型
        FundMainModelRequest fundMainModelRequest = new FundMainModelRequest();
        fundMainModelRequest.setFundMainModelStatusEnums(FundMainModelStatusEnums.EFFECTIVE);
        APIResponse<FundMainModelDTO> fundMainModelDTOAPIResponse = fundMainModelRemote.queryEffectiveFundMainModel(fundMainModelRequest);

        Preconditions.checkState(fundMainModelDTOAPIResponse.isSuccess() && !Objects.isNull(fundMainModelDTOAPIResponse.getData()),"没有查询到有效的主理人模型");

        FundMainModelDTO fundMainModelDTO = fundMainModelDTOAPIResponse.getData();

        CreatePlanRequest request = new CreatePlanRequest();
        request.setUserId(userId);
        request.setFundUserAccountId(userFundAccount.getUserFundAccountInfoId());
        request.setFundUserAccount(userFundAccount.getAccountNumber());
        request.setPlanName("A");
        //TODO MENG
        request.setPortfolioAmount(new BigDecimal(environment.getProperty("invest.min.rspAmount")));
        int day = DateTime.now().getDayOfMonth();
        if (day>28) {
            day=28;
        }
        request.setCycleDay(day);
        request.setInvestorPayId(userFundAccount.getInvestorPayId());

        PlanInfoDO planInfo = planService.savePlanInfo(request); //INIT状态

        //创建定投
        YfCreatePlanRequest yfCreatePlanRequest = new YfCreatePlanRequest();

        yfCreatePlanRequest.setCycle(YfPlanCycleEnum.getPlanCycle(request.getCycle().getType()).getYfCycle());
        yfCreatePlanRequest.setCycleDay(request.getCycleDay());
        yfCreatePlanRequest.setPortfolioCode(fundMainModelDTO.getThirdPortfolioCode());
        yfCreatePlanRequest.setRspName(request.getPlanName());
        yfCreatePlanRequest.setRspPortfolioAmount(request.getPortfolioAmount());
        yfCreatePlanRequest.setAccountNumber(request.getFundUserAccount());
        yfCreatePlanRequest.setInvestorPayId(Integer.valueOf(request.getInvestorPayId()));

        String notifyUrl = environment.getProperty("snb.api.url.domain")+"/"+environment.getProperty("ifast.plan.callback.url");

        yfCreatePlanRequest.setNotifyUrl(notifyUrl);

        log.info("开始创建定投计划：{}", yfCreatePlanRequest);

        BaseResponse<YfCreatePlanResponse> baseResponse = null;

        try {
            baseResponse = (BaseResponse<YfCreatePlanResponse>) fundPlanService.createPortfolioPlan(yfCreatePlanRequest);
        } catch (Exception e) {
            log.error("创建定投计划异常", e);
        }

        log.info("用户：{}创建定投计划完成响应：{}", request.getUserId(), baseResponse);

        Preconditions.checkNotNull(baseResponse,"用户%s开户完成创建定投计划无响应",userId);

        Preconditions.checkState(baseResponse.success(),"用户%s开户完成创建定投计划失败:%s",userId,baseResponse.getCode() + ":" + baseResponse.getMessage());

        YfCreatePlanResponse yfCreatePlanResponse = baseResponse.getData();

        //获取响应数据
        //第三方计划id
        String thirdPlanId = String.valueOf(yfCreatePlanResponse.getRspId());
        planInfo.setThirdPlanId(thirdPlanId);
        planInfo.setNextRunDate(new DateTime(Long.valueOf(yfCreatePlanResponse.getNextRunDate())).toDate());
        //持仓ID
        String portfolioId = String.valueOf(yfCreatePlanResponse.getPortfolioId());

        //4. 创建完成，保存计划任务，初始化计划统计信息，初始化用户持仓账户
        planService.afterPlanCreated(planInfo, fundMainModelDTO, portfolioId);

        //5. 暂停用户计划

        YfBasePlanRequest yfBasePlanRequest = new YfBasePlanRequest();
        yfBasePlanRequest.setAccountNumber(request.getFundUserAccount());
        yfBasePlanRequest.setRspId(Integer.valueOf(planInfo.getThirdPlanId()));

        log.info("开始暂停用户：{}定投计划：{}", userId, yfBasePlanRequest);

        BaseResponse<YfBasePlanResponse> yfbaseResponse = null;
        try {
            yfbaseResponse = (BaseResponse<YfBasePlanResponse>) fundPlanService.suspendPortfolioPlan(yfBasePlanRequest);
        } catch (Exception e) {
            log.error("暂停定投计划异常", e);
        }

        log.info("暂停用户：{}定投计划完成响应：{}", userId, yfbaseResponse);

        Preconditions.checkNotNull(yfbaseResponse,"系统暂停用户:%s定投计划:%s无响应",userId,thirdPlanId);

        Preconditions.checkState(yfbaseResponse.success()&& !Objects.isNull(yfbaseResponse.getData()),"系统暂停用户:%s定投计划:%s失败:%s",userId,thirdPlanId,baseResponse.getCode() + ":" + baseResponse.getMessage());

    }

    @Override
    public Integer syncPlanInfo(PlanInfoDO planInfoDO) throws Exception {
        String rspId = planInfoDO.getThirdPlanId();
        //查询计划信息
        //用户基金账户信息
        BaseFundRequest req = new BaseFundRequest();
        req.setUserId(planInfoDO.getUserId());
        req.setFundPlatform(FundChannelEnum.YIFENG);
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);
        Preconditions.checkState(!Objects.isNull(fundUserResponse)&&fundUserResponse.isSuccess(),"查询用户%s基金账户失败",planInfoDO.getUserId());

        String accountNumber = fundUserResponse.getData().getAccountNumber();

        YfPlanInfoQueryRequest request = new YfPlanInfoQueryRequest();
        request.setAccountNumber(accountNumber);
        request.setRspId(Integer.valueOf(rspId));
        BaseResponse<YfPlanInfoResponse> baseResponse = (BaseResponse<YfPlanInfoResponse>) fundPlanService.queryPlanInfo(request);

        Preconditions.checkState(!Objects.isNull(baseResponse)&&baseResponse.success(),"查询计划%s详情失败",rspId);

        Preconditions.checkState(CollectionUtils.isNotEmpty(baseResponse.getData().getData()),"无计划%s详情数据",rspId);

        YfPlanInfo yfPlanInfo = baseResponse.getData().getData().get(0);

        log.info("查询到计划：{}详情信息：{}",rspId,yfPlanInfo);

        String planStatus = yfPlanInfo.getRspStatus();
        if (StringUtils.isNotBlank(planStatus)) {
            PlanInfoStatusEnum status = PlanInfoStatusEnum.INIT;
            if (planStatus.equalsIgnoreCase("active")) {
                status = PlanInfoStatusEnum.ACTIVE;
            } else if (planStatus.equalsIgnoreCase("suspend")) {
                status = PlanInfoStatusEnum.SUSPEND;
            } else if (planStatus.equalsIgnoreCase("terminated")) {
                status = PlanInfoStatusEnum.STOP;
            }

            if (status != PlanInfoStatusEnum.INIT && planInfoDO.getPlanStatus() != status.getStatus()) {
                //以奕丰计划状态为主
                log.info("检测到计划：{}状态：{}和奕丰计划状态{}不一致",planInfoDO.getPlanInfoId(),planInfoDO.getPlanStatus(),status);

                return planService.updatePlanStatus(planInfoDO.getPlanInfoId(),status);
            }
        }

        return 0;
    }

    /**
     * 从db查询持仓账户信息
     * @param userId
     * @param planId
     * @param channel
     * @return
     * @author yunpeng.zhang
     */
    private PortfolioAccountBO queryPortfolioAccountFromDB(String userId, Long planId, Integer channel) {
        log.info("从db查询持仓账户信息，用户：[{}]，计划：[{}]", userId, planId);
        //1. 获取持仓账户信息
        //1.1 获取计划持仓关系列表
        List<PlanPortfolioRelDO> planPortfolioRelDOList = planService.listPlanPortfolioRel(userId, planId, channel);
        if (CollectionUtils.isEmpty(planPortfolioRelDOList)) {
            log.info("获取计划资产信息=》获取持仓关系列表为空，用户:[{}]，计划:[{}]", userId, planId);
            throw new RuntimeException("获取持仓关系列表为空");
        }
        PlanPortfolioRelDO planPortfolioRelDO = planPortfolioRelDOList.get(0);
        if (planPortfolioRelDO == null) {
            log.info("获取计划资产信息=》获取持仓关系为空，用户:[{}]，计划:[{}]", userId, planId);
            throw new RuntimeException("获取持仓关系为空");
        }
        Long planPortfolioRelId = planPortfolioRelDO.getPlanPortfolioRelId();
        if (planPortfolioRelId == null) {
            log.info("获取计划资产信息=》获取持仓关系id为空，用户:[{}]，计划:[{}]", userId, planId);
            throw new RuntimeException("获取持仓关系id为空");
        }
        //1.2 从db获取持仓账户信息
        PlanPortfolioAccountDO planPortfolioAccountDO = planService.queryPortfolioAccountFromDB(planPortfolioRelId);

        //2. 获取基金明细
        //2.1 获取用户账户id
        GetUserFundAccountInfoResponse data = getUserFundAccountInfo(userId);
        Long userFundAccountInfoId = data.getUserFundAccountInfoId();
        if (userFundAccountInfoId == null) {
            throw new RuntimeException("获取用户基金账户id为空！");
        }
        //2.2 获取用户基金明细
        UserFundDetailRequest userFundDetailRequest = new UserFundDetailRequest();
        userFundDetailRequest.setUserFundAccountInfoId(userFundAccountInfoId);
        userFundDetailRequest.setThirdPortfolioId(planPortfolioRelDO.getThirdPortfolioId());
        APIResponse<UserFundDetailResponse> userFundDetailResponse = null;
        try {
            userFundDetailResponse = userAssetAndIncomeRemote.getUserFundDetail(userFundDetailRequest);
        } catch (Exception e) {
            log.info("获取计划资产信息=》获取用户基金明细调用dubbo接口异常，用户:[{}]，计划:[{}]，请求：[{}]", userId, planId,
                    userFundDetailRequest);
            throw new RuntimeException("调用获取用户基金明细dubbo接口异常");
        }
        if (!userFundDetailResponse.isSuccess()) {
            log.info("获取计划资产信息=》获取用户基金明细失败，用户:[{}]，计划:[{}]，请求：[{}]，原因：[{}]", userId,
                    planId, userFundDetailRequest, userFundDetailResponse.getMsg());
            throw new RuntimeException("获取用户基金明细失败");
        }
        List<UserFundDetailDTO> userFundDetailDTOList = userFundDetailResponse.getData().getUserFundDetailDTOList();

        //3. 组织数据
        return establishDBPortfolioAccountData(planPortfolioAccountDO, userFundDetailDTOList);
    }

    /**
     * 组织持仓账户数据
     * @param planPortfolioAccountDO
     * @param userFundDetailDTOList
     * @return
     */
    private PortfolioAccountBO establishDBPortfolioAccountData(PlanPortfolioAccountDO planPortfolioAccountDO,
                                                               List<UserFundDetailDTO> userFundDetailDTOList) {
        PortfolioAccountBO result = new PortfolioAccountBO();

        //组合可用金额
        result.setAvailableAmount(planPortfolioAccountDO.getAvailableAmount());
        //组合最高赎回金额
        result.setMaxRedemptionAmount(planPortfolioAccountDO.getMaxRedeemableAmount());
        //组合最低赎回金额
        result.setMinRedemptionAmount(planPortfolioAccountDO.getMinRedeemableAmount());
        //组合最低保留金额
        result.setMinRetainAmount(planPortfolioAccountDO.getMinRetainAmount());

        //组合累计收益
        BigDecimal totalIncome = BigDecimal.ZERO;
        //组合昨日收益
        BigDecimal yesterdayIncome = BigDecimal.ZERO;
        //组合可用份额
        BigDecimal availableUnit = BigDecimal.ZERO;
        //组合累计投资金额
        BigDecimal totalInvestmentAmount = BigDecimal.ZERO;
        //总资产
        BigDecimal totalAmount = BigDecimal.ZERO;
        //组合在途=在途+未分配收益+不可用资产=总资产-可用金额，2018年5月31日，基金1.2期，组合在途不包含未分配收益
        BigDecimal intransitAssetsTotal = BigDecimal.ZERO;
        //总未分配收益
        BigDecimal totalUndistributedIncome = BigDecimal.ZERO;

        List<PortfolioAccountDetailBO> portfolioAccountDetailBOList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(userFundDetailDTOList)) {
            int i=0;
            BigDecimal totalProportion = BigDecimal.ZERO;
            for (UserFundDetailDTO userFundDetailDTO : userFundDetailDTOList) {
                i++;
                PortfolioAccountDetailBO accountDetail = new PortfolioAccountDetailBO();
                //基金代码
                accountDetail.setFundCode(userFundDetailDTO.getFundCode());
                //基金名称
                accountDetail.setFundName(userFundDetailDTO.getFundName());
                //基金类型
                accountDetail.setFundType(userFundDetailDTO.getFundType());
                //在途资产
                accountDetail.setIntransitAmount(userFundDetailDTO.getIntransitAssets());
                //投资金额
                accountDetail.setInvestmentAmount(userFundDetailDTO.getInvestmentAmount());
                //资产现值
                accountDetail.setCurrentValue(userFundDetailDTO.getCurrentValue());
                //可用份额
                accountDetail.setAvailableUnit(userFundDetailDTO.getAvailableUnit());
                //总份额
                accountDetail.setTotalUnit(userFundDetailDTO.getTotalUnit());
                //最新净值
                accountDetail.setNav(userFundDetailDTO.getNav());
                //净值日期
                accountDetail.setNavDate(userFundDetailDTO.getNavDate());
                //当前持仓盈亏
                accountDetail.setProfitLoss(userFundDetailDTO.getProfitLoss());
                //累计收益
                accountDetail.setTotalIncome(userFundDetailDTO.getAccumulatedProfitloss());
                //昨日收益
                accountDetail.setYesterdayIncome(userFundDetailDTO.getProfitLossDaily());
                //未分配收益
                accountDetail.setUndistributedIncome(userFundDetailDTO.getUndistributeIncome());
                // 基金可用份额*净值 / 组合可用份额
                if (planPortfolioAccountDO.getAvailableAmount().compareTo(BigDecimal.ZERO) == 0) {
                    accountDetail.setProportion(BigDecimal.ZERO);
                } else {
                    BigDecimal fundAvailableAmount = userFundDetailDTO.getAvailableUnit().multiply(userFundDetailDTO.getNav());
                    BigDecimal proportion = fundAvailableAmount.divide(planPortfolioAccountDO.getAvailableAmount(),4,BigDecimal.ROUND_DOWN).multiply(new BigDecimal(100));
                    if (i == portfolioAccountDetailBOList.size()) {
                        //最後一個
                        proportion = new BigDecimal(100).subtract(totalProportion);
                    } else {
                        totalProportion=totalProportion.add(proportion);
                    }
                    accountDetail.setProportion(proportion);
                }
                portfolioAccountDetailBOList.add(accountDetail);

                totalIncome = totalIncome.add(userFundDetailDTO.getAccumulatedProfitloss());
                yesterdayIncome = yesterdayIncome.add(userFundDetailDTO.getProfitLossDaily());
                availableUnit = availableUnit.add(userFundDetailDTO.getAvailableUnit());
                totalInvestmentAmount = totalInvestmentAmount.add(userFundDetailDTO.getInvestmentAmount());
                // 总资产
                totalAmount = totalAmount.add(userFundDetailDTO.getCurrentValue());
                // 总未分配收益
                totalUndistributedIncome = totalUndistributedIncome.add(userFundDetailDTO.getUndistributeIncome());
            }
            // 总在途=在途+未分配收益+不可用资产=总资产-可用金额
            // 2018年5月31日，基金1.2期，总在途不包含未分配收益
            intransitAssetsTotal = intransitAssetsTotal.add(totalAmount.subtract(planPortfolioAccountDO.getAvailableAmount()).
                    subtract(totalUndistributedIncome));
//            totalAmount = totalAmount.subtract(totalUndistributedIncome);
        }

        result.setTotalIntransitAmount(intransitAssetsTotal);
        result.setTotalIncome(totalIncome);
        result.setYesterdayIncome(yesterdayIncome);
        result.setAvailableUnit(availableUnit);
        result.setTotalInvestmentAmount(totalInvestmentAmount);
        result.setPortfolioAccountDetailBOList(portfolioAccountDetailBOList);
        return result;
    }

    /**
     * 组织持仓账户数据
     */
    private PortfolioAccountBO establishThirdPortfolioAccountData(String userId, Long planId, BaseResponse<YfPortfolioAccountResponse> baseResponse) {
        PortfolioAccountBO result = new PortfolioAccountBO();
        result.setUserId(userId);
        result.setPlanId(planId);

        //获取一个组合庄户
        YfPortfolioAccount yfPortfolioAccount = baseResponse.getData().getData().get(0);

        //组合可用金额
        result.setAvailableAmount(yfPortfolioAccount.getAvailableAmount());
        //组合最高赎回金额
        result.setMaxRedemptionAmount(yfPortfolioAccount.getMaxRedemptionAmount());
        //组合最低赎回金额
        result.setMinRedemptionAmount(yfPortfolioAccount.getMinRedemptionAmount());
        //组合最低保留金额
        result.setMinRetainAmount(yfPortfolioAccount.getMinRetainAmount());
        //组合类型
        result.setPortfolioType(yfPortfolioAccount.getPortfolioType());
        //是否可自由控制Y N
        result.setRebalanceEnable(yfPortfolioAccount.getRebalanceEnable());
        //是否可以赎回 Y N
        result.setSellEnable(yfPortfolioAccount.getSellEnable());

        //组合累计收益
        BigDecimal totalIncome = BigDecimal.ZERO;
        //组合昨日收益
        BigDecimal yesterdayIncome = BigDecimal.ZERO;
        //组合可用份额
        BigDecimal availableUnit = BigDecimal.ZERO;
        //组合累计投资金额
        BigDecimal totalInvestmentAmount = BigDecimal.ZERO;
        //总资产
        BigDecimal totalAmount = BigDecimal.ZERO;
        //组合在途=在途+未分配收益+不可用资产=总资产-可用金额，2018年5月31日，基金1.2期，组合在途不包含未分配收益
        BigDecimal intransitAssetsTotal = BigDecimal.ZERO;
        //总未分配收益
        BigDecimal totalUndistributedIncome = BigDecimal.ZERO;

        List<PortfolioAccountDetailBO> portfolioAccountDetailBOList = Lists.newArrayList();
        if (!Objects.isNull(yfPortfolioAccount.getFundHoldings())
                && CollectionUtils.isNotEmpty(yfPortfolioAccount.getFundHoldings().getData())) {
            int i=0;
            BigDecimal totalProportion = BigDecimal.ZERO;
            for (YfPortfolioHoldingData yfPortfolioHoldingData : yfPortfolioAccount.getFundHoldings().getData()) {
                i++;
                PortfolioAccountDetailBO accountDetail = new PortfolioAccountDetailBO();
                //基金代码
                accountDetail.setFundCode(yfPortfolioHoldingData.getFundCode());
                //基金名称
                accountDetail.setFundName(yfPortfolioHoldingData.getFundName());
                //基金类型
                accountDetail.setFundType(yfPortfolioHoldingData.getFundType());
                //在途资产
                accountDetail.setIntransitAmount(yfPortfolioHoldingData.getIntransitAssets());
                //投资金额
                accountDetail.setInvestmentAmount(yfPortfolioHoldingData.getInvestmentAmount());
                //资产现值
                accountDetail.setCurrentValue(yfPortfolioHoldingData.getCurrentValue());
                //可用份额
                accountDetail.setAvailableUnit(yfPortfolioHoldingData.getAvailableUnit());
                //总份额
                accountDetail.setTotalUnit(yfPortfolioHoldingData.getTotalUnit());
                //最新净值
                accountDetail.setNav(yfPortfolioHoldingData.getNav());
                //净值日期
                accountDetail.setNavDate(DateTimeUtil.parseDate(yfPortfolioHoldingData.getNavDate()));
                //当前持仓盈亏
                accountDetail.setProfitLoss(yfPortfolioHoldingData.getProfitLoss());
                //累计收益
                accountDetail.setTotalIncome(yfPortfolioHoldingData.getAccumulatedProfitLoss());
                //昨日收益
                accountDetail.setYesterdayIncome(yfPortfolioHoldingData.getProfitLossDaily());
                //未分配收益
                accountDetail.setUndistributedIncome(yfPortfolioHoldingData.getUndistributeMonetaryIncome());
                //占比
                // 基金可用份额*净值 / 组合可用份额
                if (yfPortfolioAccount.getAvailableAmount().compareTo(BigDecimal.ZERO) == 0) {
                    accountDetail.setProportion(BigDecimal.ZERO);
                } else {
                    BigDecimal fundAvailableAmount = yfPortfolioHoldingData.getAvailableUnit().multiply(yfPortfolioHoldingData.getNav());
                    BigDecimal proportion = fundAvailableAmount.divide(yfPortfolioAccount.getAvailableAmount(),4,BigDecimal.ROUND_DOWN).multiply(new BigDecimal(100));
                    if (i == yfPortfolioAccount.getFundHoldings().getData().size()) {
                        //最後一個
                        proportion = new BigDecimal(100).subtract(totalProportion);
                    } else {
                        totalProportion=totalProportion.add(proportion);
                    }
                    accountDetail.setProportion(proportion);
                }

                totalIncome = totalIncome.add(yfPortfolioHoldingData.getAccumulatedProfitLoss());
                yesterdayIncome = yesterdayIncome.add(yfPortfolioHoldingData.getProfitLossDaily());
                availableUnit = availableUnit.add(yfPortfolioHoldingData.getAvailableUnit());
                totalInvestmentAmount = totalInvestmentAmount.add(yfPortfolioHoldingData.getInvestmentAmount());
                // 总资产
                totalAmount = totalAmount.add(yfPortfolioHoldingData.getCurrentValue());
                // 总未分配收益
                totalUndistributedIncome = totalUndistributedIncome.add(yfPortfolioHoldingData.getUndistributeMonetaryIncome());

                portfolioAccountDetailBOList.add(accountDetail);
            }
            // 总在途=在途+未分配收益+不可用资产=总资产-可用金额
            // 2018年5月31日，基金1.2期，总在途不包含未分配收益，改之前如上
            intransitAssetsTotal = intransitAssetsTotal.add(totalAmount.subtract(yfPortfolioAccount.getAvailableAmount()).
                    subtract(totalUndistributedIncome));
//            totalAmount = totalAmount.subtract(totalUndistributedIncome);
        }

        result.setTotalIntransitAmount(intransitAssetsTotal);
        result.setTotalIncome(totalIncome);
        result.setYesterdayIncome(yesterdayIncome);
        result.setAvailableUnit(availableUnit);
        result.setTotalInvestmentAmount(totalInvestmentAmount);
        result.setPortfolioAccountDetailBOList(portfolioAccountDetailBOList);
        return result;
    }

    @Override
    public void updatePlanNextRunDate(String rspId, Date nextRunDate, FundChannelEnum channel) throws Exception {

        //根据rspId和channel 查询计划信息
        PlanInfoDO planInfoDO = planService.queryPlanByThirdPlanId(rspId,channel);

        Preconditions.checkState(!Objects.isNull(planInfoDO),"查询第三方计划%s,channel:%s失败",rspId,channel);

        if (!Objects.isNull(nextRunDate)) {
            planService.updatePlanNextRunDate(planInfoDO.getPlanInfoId(),nextRunDate);
        } else {
            //查询计划信息
            //用户基金账户信息
            BaseFundRequest req = new BaseFundRequest();
            req.setUserId(planInfoDO.getUserId());
            req.setFundPlatform(FundChannelEnum.YIFENG);
            APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);
            Preconditions.checkState(!Objects.isNull(fundUserResponse)&&fundUserResponse.isSuccess(),"查询用户%s基金账户失败",planInfoDO.getUserId());

            String accountNumber = fundUserResponse.getData().getAccountNumber();

            YfPlanInfoQueryRequest request = new YfPlanInfoQueryRequest();
            request.setAccountNumber(accountNumber);
            request.setRspId(Integer.valueOf(rspId));
            BaseResponse<YfPlanInfoResponse> baseResponse = (BaseResponse<YfPlanInfoResponse>) fundPlanService.queryPlanInfo(request);

            Preconditions.checkState(!Objects.isNull(baseResponse)&&baseResponse.success(),"查询计划%s详情失败",rspId);

            Preconditions.checkState(CollectionUtils.isNotEmpty(baseResponse.getData().getData()),"无计划%s详情数据",rspId);

            YfPlanInfo yfPlanInfo = baseResponse.getData().getData().get(0);

            log.info("查询到计划：{}详情信息：{}",rspId,yfPlanInfo);

            if (StringUtils.isNotBlank(yfPlanInfo.getNextRunDate())) {

                nextRunDate = new DateTime(Long.valueOf(yfPlanInfo.getNextRunDate())).toDate();

                planService.updatePlanNextRunDate(planInfoDO.getPlanInfoId(),nextRunDate);
            }

        }

    }

    @Override
    public void syncPortfolioAccount(AccountSyncMessage message) throws Exception {

        log.info("同步账户信息，{}",message);

        String userId = message.getUserId();
        String accountNumber = message.getAccountNumber();

        //查询账户下所有持仓
        YfPortfolioAccountRequest yfPortfolioAccountRequest = new YfPortfolioAccountRequest();
        yfPortfolioAccountRequest.setAccountNumber(accountNumber);
        //yfPortfolioAccountRequest.setPortfolioId(Integer.valueOf(planPortfolioRelDO.getThirdPortfolioId()));

        BaseResponse<YfPortfolioAccountResponse> baseResponse = (BaseResponse<YfPortfolioAccountResponse>) fundPlanService.queryPortfolioAccount(yfPortfolioAccountRequest);

        Preconditions.checkNotNull(baseResponse,"查询用户:%s账户:%s持有资产无响应",userId,accountNumber);

        Preconditions.checkState(baseResponse.success(),"查询用户:%s账户:%s持有资产响应失败:%s",userId,accountNumber,baseResponse.getMessage());

        Preconditions.checkNotNull(baseResponse.getData(),"查询用户:%s账户:%s持有资产无响应数据",userId,accountNumber);

        //账户可赎回资产，账户下持仓可赎回资产累加
        BigDecimal userAvailableAmount = BigDecimal.ZERO;

        List<YfPortfolioAccount> yfPortfolioAccountList = baseResponse.getData().getData();

        if (CollectionUtils.isNotEmpty(yfPortfolioAccountList)) {

            for (YfPortfolioAccount yfPortfolioAccount : yfPortfolioAccountList) {
                //组合总资产 （组合下基金资产现值累加）
                BigDecimal portfolioTotalAmount = BigDecimal.ZERO;
                //组合可用金额
                BigDecimal portfolioAvailableAmount = yfPortfolioAccount.getAvailableAmount();
                //组合在途资产
                BigDecimal portfolioIntransitAmount  = yfPortfolioAccount.getIntransitAssetsTotal();
                //组合不可用资产（总资产-可用资产-在途）
                BigDecimal portfolioUnAvailableAmount = BigDecimal.ZERO;
                //持仓ID
                String portfolioId = String.valueOf(yfPortfolioAccount.getPortfolioId());
                //组合代码
                String portfolioCode = yfPortfolioAccount.getModelPortfolioCode();
                //组合最高赎回金额
                BigDecimal maxRedemptionAmount = yfPortfolioAccount.getMaxRedemptionAmount();
                //组合最低赎回金额
                BigDecimal minRedemptionAmount = yfPortfolioAccount.getMinRedemptionAmount();
                //组合最低保留金额
                BigDecimal minRetainAmount = yfPortfolioAccount.getMinRetainAmount();
                //组合成本
                BigDecimal portfolioInvestAmount = BigDecimal.ZERO;

                userAvailableAmount = userAvailableAmount.add(portfolioAvailableAmount);

                if (!Objects.isNull(yfPortfolioAccount.getFundHoldings())
                        && CollectionUtils.isNotEmpty(yfPortfolioAccount.getFundHoldings().getData())) {
                    //每个持仓下的基金情况
                    for (YfPortfolioHoldingData yfPortfolioHoldingData : yfPortfolioAccount.getFundHoldings().getData()) {

                        //基金累计收益
                        BigDecimal fundAccumulatedProfitLoss = yfPortfolioHoldingData.getAccumulatedProfitLoss();
                        //可处理份额
                        BigDecimal fundAvailableUnit = yfPortfolioHoldingData.getAvailableUnit();
                        //基金资产现值
                        BigDecimal fundCurrentValue = yfPortfolioHoldingData.getCurrentValue();

                        //基金代码
                        String fundCode = yfPortfolioHoldingData.getFundCode();
                        //基金名称
                        String fundName = yfPortfolioHoldingData.getFundName();
                        //基金类型
                        String fundType = yfPortfolioHoldingData.getFundType();
                        //基金在途资产
                        BigDecimal fundIntransitAssets = yfPortfolioHoldingData.getIntransitAssets();
                        //基金成本
                        BigDecimal fundInvestAmount = yfPortfolioHoldingData.getInvestmentAmount();
                        //最新净值
                        BigDecimal fundNav = yfPortfolioHoldingData.getNav();
                        //净值日期
                        if (StringUtils.isNotBlank(yfPortfolioHoldingData.getNavDate())) {
                            Date fundNavDate = new DateTime(Long.valueOf(yfPortfolioHoldingData.getNavDate())).toDate();
                        }
                        //持仓盈亏
                        BigDecimal fundProfitLoss = yfPortfolioHoldingData.getProfitLoss();
                        //昨日收益
                        BigDecimal fundProfitLossDaily = yfPortfolioHoldingData.getProfitLossDaily();
                        //持有总份额
                        BigDecimal fundTotalUnit = yfPortfolioHoldingData.getTotalUnit();
                        //未分配收益
                        BigDecimal fundUndistributeMonetaryIncome = yfPortfolioHoldingData.getUndistributeMonetaryIncome();
                        //基金不可用金额(资产现值-可用份额*净值-在途资产)，在奕丰接口中，此部分资产既不在可用资产，也不在在途
                        BigDecimal fundUnAvailableAmount = fundCurrentValue.subtract(fundAvailableUnit.multiply(fundNav)).subtract(fundIntransitAssets).setScale(2,BigDecimal.ROUND_HALF_UP);

                        portfolioTotalAmount = portfolioTotalAmount.add(fundCurrentValue);
                        portfolioUnAvailableAmount = portfolioUnAvailableAmount.add(fundUnAvailableAmount);
                        portfolioInvestAmount = portfolioInvestAmount.add(fundInvestAmount);
                    }

                }

                //根据portfolioId和userId 查询计划组合
                PlanPortfolioRelDO planPortfolioRelDO = planService.queryUserPortfolioRelByPortfolioId(userId,portfolioId,FundChannelEnum.YIFENG);
                if (Objects.isNull(planPortfolioRelDO)) {
                    log.error("同步用户：{}组合账户：{}，发现不是计划内持仓数据，portfolioId：{}",userId,accountNumber,portfolioId);
                    continue;
                }

                Long planPortfolioRelId = planPortfolioRelDO.getPlanPortfolioRelId();

                //查询组合账户
                PlanPortfolioAccountDO planPortfolioAccountDO = planService.queryUserAccountByPlanPortfolioRelId(planPortfolioRelId);

                Preconditions.checkNotNull(planPortfolioAccountDO,"根据planPortfolioRelId:%s未查询到计划账户信息",planPortfolioRelId);

                PlanPortfolioAccountDO accountDO = new PlanPortfolioAccountDO();
                accountDO.setTotalAmount(portfolioTotalAmount);
                accountDO.setTotalInvestAmount(portfolioInvestAmount);
                accountDO.setAvailableAmount(portfolioAvailableAmount);
                accountDO.setUnAvailableAmount(portfolioUnAvailableAmount);
                accountDO.setTotalIntransitAmount(portfolioIntransitAmount);
                accountDO.setPlanPortfolioAccountId(planPortfolioAccountDO.getPlanPortfolioAccountId());
                accountDO.setMaxRedeemableAmount(maxRedemptionAmount);
                accountDO.setMinRedeemableAmount(minRedemptionAmount);
                accountDO.setMinRetainAmount(minRetainAmount);

                //更新组合账户信息
                planService.updatePlanPortfolioAccount(accountDO);
            }

        }

    }

    /**
     * 获取计划收益信息
     * @param userId
     * @param planId
     * @author yunpeng.zhang
     */
    @Override
    public PlanIncomeBO getPlanIncomeInfo(String userId, Long planId) {
        //1.1 根据用户id和计划id查询持仓关系列表
        List<PlanPortfolioRelDO> planPortfolioRelList = planService.listPlanPortfolioRel(userId, planId, FundChannelEnum.YIFENG.getChannel());
        if (CollectionUtils.isEmpty(planPortfolioRelList)) {
            throw new RuntimeException("获取计划持仓关系列表出错！");
        }
        //1.2 获取计划持仓id
        Long planPortfolioRelId = planPortfolioRelList.get(0).getPlanPortfolioRelId();
        if (planPortfolioRelId == null) {
            throw new RuntimeException("获取到计划持仓id为空！");
        }

        //2 获取计划收益信息
        return planService.getPlanIncome(userId, planPortfolioRelId);
    }

    @Override
    public void syncPlanExecuteRecord(PlanInfoDO planInfoDO) throws Exception {

        String userId = planInfoDO.getUserId();
        Long planInfoId = planInfoDO.getPlanInfoId();
        String thirdPlanId = planInfoDO.getThirdPlanId();

        log.info("同步用户：{}，计划：{}定投计划执行记录",userId,planInfoId);

        //查询用户基金账户信息

        BaseFundRequest req = new BaseFundRequest();
        req.setUserId(userId);
        req.setFundPlatform(FundChannelEnum.YIFENG);
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);
        Preconditions.checkState(!Objects.isNull(fundUserResponse)&&fundUserResponse.isSuccess(),"查询用户%s基金账户失败",planInfoDO.getUserId());

        String accountNumber = fundUserResponse.getData().getAccountNumber();

        //查询用户定投计划的执行历史记录
        YfBasePlanRequest request = new YfBasePlanRequest();
        request.setAccountNumber(accountNumber);
        request.setRspId(Integer.valueOf(thirdPlanId));

        log.info("查询用户:{}，计划:{}执行记录",request);

        BaseResponse<YfPlanExecuteRecord[]> baseResponse = (BaseResponse<YfPlanExecuteRecord[]>) fundPlanService.queryPlanExecuteRecord(request);

        Preconditions.checkNotNull(baseResponse,"查询用户:%s账户:%s计划:%s执行记录无响应",userId,accountNumber,thirdPlanId);

        Preconditions.checkState(baseResponse.success(),"查询用户:%s账户:%s计划:%s执行记录响应失败:%s",userId,accountNumber,thirdPlanId,baseResponse.getMessage());

        List<YfPlanExecuteRecord> yfPlanExecuteRecordList = Lists.newArrayList();
        YfPlanExecuteRecord[] yfPlanExecuteRecords = baseResponse.getData();
        if (!Objects.isNull(baseResponse.getData()) || baseResponse.getData().length !=0) {
            yfPlanExecuteRecordList = Arrays.asList(yfPlanExecuteRecords);
        }

        if (CollectionUtils.isNotEmpty(yfPlanExecuteRecordList)) {
            planService.savePlanExecuteRecord(planInfoDO,yfPlanExecuteRecordList,accountNumber);
        }

    }

    /**
     * 获取计划持仓组合代码
     *
     * @param userId
     * @param planId
     * @return
     * @author yunpeng.zhang
     */
    private String getPlanPortfolioId(String userId, Long planId) {
        //1 根据用户id和计划id查询持仓关系列表
        List<PlanPortfolioRelDO> planPortfolioRelList = planService.listPlanPortfolioRel(userId, planId, FundChannelEnum.YIFENG.getChannel());
        if (CollectionUtils.isEmpty(planPortfolioRelList)) {
            throw new RuntimeException("获取计划持仓关系列表出错！");
        }
        //2 获取计划持仓组合代码
        String portfolioCode = planPortfolioRelList.get(0).getThirdPortfolioId();
        if (StringUtils.isEmpty(portfolioCode)) {
            throw new RuntimeException("获取到计划持仓组合代码为空！");
        }
        return portfolioCode;
    }

    private GetUserFundAccountInfoResponse getUserFundAccountInfo(String userId) {
        BaseFundRequest request = new BaseFundRequest();
        request.setFundPlatform(FundChannelEnum.YIFENG);
        request.setUserId(userId);
        APIResponse<GetUserFundAccountInfoResponse> userFundAccountInfoResponse = null;
        try {
            userFundAccountInfoResponse = fundUserRemote.getUserFundAccountInfo(request);
        } catch (BusinessException e) {
            throw new RuntimeException("获取用户基金账户信息失败");
        }
        if (!userFundAccountInfoResponse.isSuccess()) {
            throw new RuntimeException("获取用户基金账户信息失败，" + userFundAccountInfoResponse.getMsg());
        }
        GetUserFundAccountInfoResponse data = userFundAccountInfoResponse.getData();
        if (data == null) {
            throw new RuntimeException("获取用户基金账户信息为空！");
        }
        return data;
    }

}
