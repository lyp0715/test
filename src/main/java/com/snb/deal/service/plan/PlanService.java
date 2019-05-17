package com.snb.deal.service.plan;

import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.plan.CreatePlanRequest;
import com.snb.deal.api.dto.plan.ModifyPlanRequest;
import com.snb.deal.api.dto.plan.PlanDetailRequest;
import com.snb.deal.api.dto.plan.portfolio.PortfolioAccountRequest;
import com.snb.deal.api.enums.plan.PlanInfoStatusEnum;
import com.snb.deal.bo.plan.PlanIncomeBO;
import com.snb.deal.bo.plan.PortfolioAccountBO;
import com.snb.deal.entity.incomesync.FundIncomeSyncDO;
import com.snb.deal.entity.plan.*;
import com.snb.fund.api.dto.mainmodel.FundMainModelDTO;
import com.snb.third.yifeng.dto.plan.YfPlanExecuteRecord;

import java.util.Date;
import java.util.List;

/**
 * 计划相关本地服务
 */
public interface PlanService {

    /**
     * 保存用户计划配置
     * @param request
     * @return
     */
    PlanInfoDO savePlanInfo(CreatePlanRequest request);

    /**
     * 计划创建完成
     * @param planInfo
     */
    void afterPlanCreated(PlanInfoDO planInfo, FundMainModelDTO fundMainModel, String portfolioId) throws Exception;


    /**
     * 更新计划状态
     * @param planId 计划id
     * @param planInfoStatusEnum 计划状态
     * @return
     */
    int updatePlanStatus(long planId, PlanInfoStatusEnum planInfoStatusEnum);

    /**
     * 保存第三方计划id
     * @param planId 计划id
     * @param thirdPlanId 第三方计划id
     * @return
     */
    int saveThirdPlanId(long planId, String thirdPlanId);

    /**
     * 更新计划信息
     * @param planInfo 计划信息
     * @return
     */
    int updatePlanInfo(PlanInfoDO planInfo);

    /**
     * 保存计划持仓信息
     * @param planPortfolioRel 计划持仓信息
     * @return
     */
    PlanPortfolioRelDO savePlanPortfolio(PlanPortfolioRelDO planPortfolioRel);

    /**
     * 根据业务主键ID查询计划信息
     * @param planId
     * @return
     */
    PlanInfoDO queryPlanInfoById(Long planId);
    /**
     * 根据计划id查询计划持仓账户信息
     * @param planId 计划id
     * @author yunpeng.zhang
     * @return
     */
    PlanPortfolioAccountDO getPlanPortfolioAccount(Long planId);

    /**
     * 根据计划id查询计划统计信息
     * @param request
     * @author yunpeng.zhang
     * @return
     */
    PlanStatisticsDO getPlanStatistics(PlanDetailRequest request);

    /**
     * 根据计划id查询计划信息
     * @param request
     * @author yunpeng.zhang
     * @return
     */
    PlanInfoDO getPlanInfo(PlanDetailRequest request);

    /**
     * 分页查询可用的用户计划信息
     * @return
     */
    List<PlanInfoDO> pageAvailablePlanByRunDate(Date nextRunDate,Integer pageNo, Integer pageSize);

    /**
     * 计划修改成功后，修改计划信息，修改计划任务
     * @param planInfo 原计划信息
     * @param request 修改请求参数
     */
    void afterPlanModified(PlanInfoDO planInfo, ModifyPlanRequest request) throws Exception;

    /**
     * 查询用户定投计划
     * @param userId 用户id
     * @param planId 计划id
     * @return
     */
    PlanInfoDO queryUserPlanInfoById(String userId, Long planId);

    /**
     * 计划暂停，修改计划状态，处理计划任务
     * @param planInfo
     */
    void afterPlanSuspended(PlanInfoDO planInfo) throws Exception;

    /**
     * 计划重启后，修改计划状态，处理计划任务
     * @param planInfo
     * @throws Exception
     */
    void afterPlanRestarted(PlanInfoDO planInfo) throws Exception;

    /**
     * 根据用户id和计划id查询计划持仓关系列表
     *
     * @author yunpeng.zhang
     * @param userId
     * @param planId
     * @return
     */
    List<PlanPortfolioRelDO> listPlanPortfolioRel(String userId, Long planId, Integer channel);

    /**
     * 查询用户计划持仓组合，一个计划可能会有多个组合，目前只有一个
     * @param userId 用户id
     * @param planId 计划id
     * @return
     */
    List<PlanPortfolioModel> queryUserPlanPortfolioModel(String userId, Long planId);

    /**
     * 查询用户持仓账户
     * @param planPortfolioRelId
     * @return
     */
    PlanPortfolioAccountDO queryUserAccountByPlanPortfolioRelId(Long planPortfolioRelId);

    /**
     * 更新计划账户
     * @param planPortfolioAccount
     * @return
     */
    int updatePlanPortfolioAccount(PlanPortfolioAccountDO planPortfolioAccount);

    /**
     * 查询可计息的计划持仓列表
     * @param channelEnum
     * @return
     */
    List<PlanPortfolioRelDO> queryPlanPortfolioRelList(FundChannelEnum channelEnum);

    /**
     *
     * @param userId
     * @param syncDate
     * @return
     */
    FundIncomeSyncDO queryFundIncomeSyncByDate(String userId, Date syncDate);

    /**
     * 根据计划id查询计划信息
     *
     * @param planId 计划id
     * @return
     * @author yunpeng.zhang
     */
    PlanInfoDO getPlanInfo(Long planId);

    /**
     * 根据第三方计划id和渠道查询定投计划
     * @param thirdPlanId 三方计划id
     * @param channel 渠道
     * @return
     */
    PlanInfoDO queryPlanByThirdPlanId(String thirdPlanId, FundChannelEnum channel);

    /**
     * 从db查询组合账户信息
     * @param planPortfolioRelId
     * @return
     * @author yunpeng.zhang
     */
    PlanPortfolioAccountDO queryPortfolioAccountFromDB(Long planPortfolioRelId);

    /**
     * 根据用户id查询计划列表
     *
     * @param userId
     * @author yunpeng.zhang
     */
    List<PlanInfoDO> listPlanInfo(String userId);

    /**
     * 更新计划下次执行日期
     * @param planInfoId 计划id
     * @param nextRunDate 下次执行日期
     * @return
     */
    int updatePlanNextRunDate(Long planInfoId, Date nextRunDate);

    /**
     * 更新计划第三方持仓ID
     * @param userId 用户ID
     * @param planInfoId 计划ID
     * @param portfolioId 第三方持仓ID
     * @return
     * @throws Exception
     */
    int updatePlanThirdPortfolioId(String userId, Long planInfoId, String portfolioId, FundChannelEnum channel) throws Exception;

    /**
     * 计划下次执行提醒
     * @param planInfoDO
     * @throws Exception
     */
    void planNextRunRemind(PlanInfoDO planInfoDO) throws Exception;

    /**
     * 获取计划收益
     * @param userId
     * @param planPortfolioRelId
     * @author yunpeng.zhang
     */
    PlanIncomeBO getPlanIncome(String userId, Long planPortfolioRelId);

    /**
     * 根据用户ID和持仓ID查询
     * @param userId
     * @param thirdPortfolioId
     * @param channel
     * @return
     */
    PlanPortfolioRelDO queryUserPortfolioRelByPortfolioId(String userId, String thirdPortfolioId, FundChannelEnum channel);

    /**
     * 保存定投计划执行记录
     * @param planInfoDO
     * @param executeRecordList
     * @throws Exception
     */
    void savePlanExecuteRecord(PlanInfoDO planInfoDO, List<YfPlanExecuteRecord> executeRecordList, String accountNumber) throws Exception;

    /**
     * 查询用户计划最后一条执行记录
     * @param userId 用户ID
     * @param planInfoId 计划ID
     * @return
     */
    PlanExecuteRecordDO queryUserLastestExecuteRecord(String userId, Long planInfoId);

    /**
     * 查询用户被暂停的计划
     * @param userId
     * @return
     */
    PlanInfoDO queryUserSuspenPlan(String userId);

    List<PlanPortfolioModel> queryUserPlanPortfolioModel(String userId);

    List<PlanInfoDO> pagePlanInfo(Integer pageNo, Integer pageSize);

    List<PlanInfoDO> pageAvailablePlanBeforeRunDate(Date nextRunDate,Integer pageNo, Integer pageSize);
}
