package com.snb.deal.biz.plan;

import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AccountSyncMessage;
import com.snb.deal.api.dto.plan.*;
import com.snb.deal.bo.plan.PlanIncomeBO;
import com.snb.deal.bo.plan.PortfolioAccountBO;
import com.snb.deal.entity.incomesync.FundIncomeSyncDO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.deal.entity.plan.PlanPortfolioRelDO;

import java.util.Date;

/**
 * 计划相关业务操作
 */
public interface PlanBiz {

    /**
     * 创建计划
     * @param request
     * @return
     */
    APIResponse<PlanResponse> createPlan(CreatePlanRequest request);

    /**
     * 修改计划
     * @param request
     * @return
     */
    APIResponse<PlanResponse> modifyPlan(ModifyPlanRequest request);


    /**
     *  暂停计划
     * @return
     */
    APIResponse<PlanResponse> suspendPlan(SuspendPlanRequest request);

    /**
     * 重启计划
     * @return
     */
    APIResponse<PlanResponse> restartPlan(RestartPlanRequest request);

    /**
     * 同步用户收益
     * @param planPortfolioRel
     * @param accountNumber 奕丰账户
     * @return
     */
    void syncPortfolioIncome(PlanPortfolioRelDO planPortfolioRel, String accountNumber, FundIncomeSyncDO fundIncomeSync);

    /**
     * 同步基金持仓账户
     * @param request
     */
    void syncPlanAccount(PlanAccountSyncRequest request);

    /**
     * 根据用户id和计划id查询计划资产详情
     * @param userId 用户id
     * @param planId 计划id
     * @author yunpeng.zhang
     */
    PlanAssetDetailResponse getPlanAssetDetail(String userId, Long planId) throws Exception;

    /**
     * 获取计划可赎回资产、确认中资产
     * @param userId 用户id
     * @param planId 计划id
     * @param channel 渠道
     * @return
     * @author yunpeng.zhang
     */
    PlanAssetInfoResponse getPlanAssetInfo(String userId, Long planId, Integer channel) throws Exception;

    /**
     * 更新计划下次执行日期
     * @param rspId 第三方计划id
     * @param nextRunDate 下次执行时间
     * @throws Exception
     */
    void updatePlanNextRunDate(String rspId, Date nextRunDate, FundChannelEnum channel) throws Exception;

    /**
     * 同步组合账户信息
     * @param message
     * @throws Exception
     */
    void syncPortfolioAccount(AccountSyncMessage message) throws Exception;

    /**
     * 获取计划收益信息
     * @param userId
     * @param planId
     * @author yunpeng.zhang
     */
    PlanIncomeBO getPlanIncomeInfo(String userId, Long planId);

    /**
     * 同步定投计划执行记录
     * @throws Exception
     */
    void syncPlanExecuteRecord(PlanInfoDO planInfoDO) throws Exception;

    /**
     * 实时查询持仓账户信息
     * @param userId
     * @param planId
     * @return
     * @throws Exception
     */
    PortfolioAccountBO queryPortfolioAccountFromThird(String userId, Long planId) throws Exception;

    /**
     * 修改用户计划支付代码
     * @param userId 用户id
     * @param investorPayId 支付代码
     * @return
     */
    void modifyInvestorPayId(String userId, Integer investorPayId) throws Exception;

    /**
     * 初始化定投计划
     * 1. 初始化定投计划
     * 2. 保存计划持仓相关数据
     * 3. 暂停定投计划
     * @param userId
     * @throws Exception
     */
    void initPlan(String userId) throws Exception;


    /**
     * 根据用户id查询用户定投记录
     * @param request
     * @author zhangwenting
     */
    PlanAutoInvestResponse getPlanAutoInvestInfo(PlanAutoInvestRequest request) throws Exception;


    Integer syncPlanInfo(PlanInfoDO planInfoDO) throws Exception;

}
