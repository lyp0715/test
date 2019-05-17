package com.snb.deal.service.impl.plan;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jianlc.event.Event;
import com.jianlc.event.EventMessageContext;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.PlanCreatedMessage;
import com.snb.common.mq.bean.PlanExecuteRecordSyncMessage;
import com.snb.common.mq.bean.PlanNextRunRemindMessage;
import com.snb.deal.api.dto.plan.CreatePlanRequest;
import com.snb.deal.api.dto.plan.ModifyPlanRequest;
import com.snb.deal.api.dto.plan.PlanDetailRequest;
import com.snb.deal.api.dto.plan.portfolio.PortfolioAccountRequest;
import com.snb.deal.api.enums.plan.PlanInfoStatusEnum;
import com.snb.deal.bo.plan.PlanIncomeBO;
import com.snb.deal.bo.plan.PortfolioAccountBO;
import com.snb.deal.bo.plan.PortfolioAccountDetailBO;
import com.snb.deal.entity.incomesync.FundIncomeSyncDO;
import com.snb.deal.entity.plan.*;
import com.snb.deal.entity.portfolio.PortfolioDailyIncomeDO;
import com.snb.deal.mapper.plan.*;
import com.snb.deal.mapper.portfolio.PortfolioDailyIncomeMapper;
import com.snb.deal.service.plan.PlanService;
import com.snb.fund.api.dto.mainmodel.FundMainModelDTO;
import com.snb.third.api.BaseResponse;
import com.snb.third.api.plan.FundPlanService;
import com.snb.third.yifeng.dto.plan.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 计划相关本地服务
 */
@Service
@Slf4j
public class PlanServiceImpl implements PlanService {

    @Autowired
    private PlanInfoMapper planInfoMapper;
    @Autowired
    private PlanPortfolioRelMapper planPortfolioRelMapper;
    @Autowired
    private GuidCreater guidCreater;
    @Autowired
    private PlanStatisticsMapper planStatisticsMapper;
    @Autowired
    private PlanPortfolioAccountMapper portfolioAccountMapper;
    @Resource
    FundPlanService fundPlanService;
    @Autowired
    AmqpTemplate amqpTemplate;
    @Autowired
    private PortfolioDailyIncomeMapper portfolioDailyIncomeMapper;
    @Autowired
    private PlanExecuteRecordMapper planExecuteRecordMapper;


    @Override
    public PlanInfoDO savePlanInfo(CreatePlanRequest request) {

        //保存计划
        PlanInfoDO planInfo = new PlanInfoDO();
        planInfo.setPlanInfoId(guidCreater.getUniqueID());//生成
        planInfo.setUserId(request.getUserId());
        planInfo.setChannel(request.getFundChannelEnum().getChannel());
        planInfo.setCycle(request.getCycle().getType());
        planInfo.setCycleDay(request.getCycleDay());
        planInfo.setFundUserAccountId(request.getFundUserAccountId());
        planInfo.setImplement(request.isImplement()?1:0);
        planInfo.setPostponeDeduction(request.isPostponeDeduction()?1:0);
        planInfo.setPortfolioAmount(request.getPortfolioAmount());
        planInfo.setPortfolioYear(request.getPortfolioYear());
        planInfo.setTargetAmount(request.getTargetAmount());
        planInfo.setPlanName(request.getPlanName());
        planInfo.setPlanStatus(PlanInfoStatusEnum.INIT.getStatus());//初始化状态，此计划不可用
        planInfo.setThirdPlanId("");//暂无

        planInfoMapper.insert(planInfo);

        return planInfo;
    }

    @Event(reliability = true,
            eventType = "'planCreateComplete'",
            eventId = "#message.getPlanId()",
            queue = "",
            exchange = "exchange.plan.created",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void afterPlanCreated(PlanInfoDO planInfo, FundMainModelDTO fundMainModel, String portfolioId) throws Exception{

        //1. 更新第三方计划id和计划状态
        PlanInfoDO newPlanInfo = new PlanInfoDO();
        newPlanInfo.setPlanInfoId(planInfo.getPlanInfoId());
        newPlanInfo.setNextRunDate(planInfo.getNextRunDate());
        newPlanInfo.setThirdPlanId(planInfo.getThirdPlanId());
        //系统暂停状态
        newPlanInfo.setPlanStatus(PlanInfoStatusEnum.SYSTEM_SUSPEND.getStatus());

        planInfoMapper.updatePlanInfo(newPlanInfo);

        //2. 保存计划持仓关系
        PlanPortfolioRelDO planPortfolioRel = new PlanPortfolioRelDO();
        planPortfolioRel.setUserId(planInfo.getUserId());
        planPortfolioRel.setChannel(planInfo.getChannel());
        planPortfolioRel.setPlanId(planInfo.getPlanInfoId());
        planPortfolioRel.setMainModelId(fundMainModel.getFundMainModelId());
        planPortfolioRel.setPlanPortfolioRelId(guidCreater.getUniqueID());
        planPortfolioRel.setThirdPortfolioId(portfolioId);

        planPortfolioRelMapper.insert(planPortfolioRel);

        //3. 初始化持仓账户
        PlanPortfolioAccountDO portfolioAccount = new PlanPortfolioAccountDO();
        portfolioAccount.setPlanPortfolioRelId(planPortfolioRel.getPlanPortfolioRelId());
        portfolioAccount.setPlanPortfolioAccountId(guidCreater.getUniqueID());//生成
        portfolioAccount.setUserId(planInfo.getUserId());

        portfolioAccountMapper.insert(portfolioAccount);

        //发送消息
        PlanCreatedMessage message = new PlanCreatedMessage();
        message.setUserId(planInfo.getUserId());
        message.setPlanId(planInfo.getPlanInfoId());
        message.setNextRunDate(planInfo.getNextRunDate());
        message.setPlanCreateTime(new Date());
        message.setThirdPlanId(planInfo.getThirdPlanId());

        EventMessageContext.addMessage(message);
    }

    @Override
    public int updatePlanStatus(long planId, PlanInfoStatusEnum planInfoStatusEnum) {
        return planInfoMapper.updatePlanStatus(planId,planInfoStatusEnum.getStatus());
    }

    @Override
    public int saveThirdPlanId(long planId, String thirdPlanId) {
        return 0;
    }

    @Override
    public int updatePlanInfo(PlanInfoDO planInfo) {
        return 0;
    }

    @Override
    public PlanPortfolioRelDO savePlanPortfolio(PlanPortfolioRelDO planPortfolioRel) {
        return null;
    }

    @Override
    public PlanInfoDO queryPlanInfoById(Long planId) {
        return planInfoMapper.getByPlanInfoId(planId);
    }

    @Override
    public PlanPortfolioAccountDO getPlanPortfolioAccount(Long planId) {
        // 目前版本一个计划只会对应有一个持仓关系（2018年4月14日）
        List<PlanPortfolioRelDO> planPortfolioRelList = planPortfolioRelMapper.listPlanPortfolioRel(null, planId, null);
        if (CollectionUtils.isEmpty(planPortfolioRelList)) {
            throw new RuntimeException("计划持仓关系表业务id为空");
        }
        return portfolioAccountMapper.selectByPlanPortfolioRelId(planPortfolioRelList.get(0).getPlanPortfolioRelId());
    }

    @Override
    public PlanStatisticsDO getPlanStatistics(PlanDetailRequest request) {
        return planStatisticsMapper.selectByPlanInfoId(request.getPlanInfoId(), request.getUserId());
    }

    @Override
    public PlanInfoDO getPlanInfo(PlanDetailRequest request) {
        return planInfoMapper.getByPlanInfoId(request.getPlanInfoId());
    }

    @Override
    public List<PlanInfoDO> pageAvailablePlanByRunDate(Date nextRunDate,Integer pageNo, Integer pageSize) {

        if (pageNo==null || pageNo<0){
            pageNo=0;
        }
        return planInfoMapper.pageAvailablePlanByRunDate(nextRunDate,pageNo*pageSize,pageSize);
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void afterPlanModified(PlanInfoDO planInfo, ModifyPlanRequest request) throws Exception{

        //修改完成，更新计划信息，更新计划任务
        PlanInfoDO newPlanInfo = new PlanInfoDO();
        newPlanInfo.setPlanInfoId(planInfo.getPlanInfoId());
        newPlanInfo.setCycle(request.getCycle().getType());
        newPlanInfo.setCycleDay(request.getCycleDay());
        newPlanInfo.setPortfolioAmount(request.getPortfolioAmount());
        newPlanInfo.setNextRunDate(planInfo.getNextRunDate());
        newPlanInfo.setPlanStatus(PlanInfoStatusEnum.ACTIVE.getStatus());

        planInfoMapper.updatePlanInfo(newPlanInfo);

    }

    @Override
    public PlanInfoDO queryUserPlanInfoById(String userId, Long planId) {
        return planInfoMapper.getUserPlanInfoById(userId,planId);
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void afterPlanSuspended(PlanInfoDO planInfo) throws Exception {

        //更新计划状态
        planInfoMapper.updatePlanStatus(planInfo.getPlanInfoId(),PlanInfoStatusEnum.SUSPEND.getStatus());

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void afterPlanRestarted(PlanInfoDO planInfo) throws Exception {

        PlanInfoDO newPlanInfo = new PlanInfoDO();
        newPlanInfo.setPlanInfoId(planInfo.getPlanInfoId());
        newPlanInfo.setPlanStatus(PlanInfoStatusEnum.ACTIVE.getStatus());
        newPlanInfo.setNextRunDate(planInfo.getNextRunDate());

        //更新计划状态
        planInfoMapper.updatePlanInfo(newPlanInfo);

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED)
    public List<PlanPortfolioRelDO> listPlanPortfolioRel(String userId, Long planId, Integer channel) {
        return planPortfolioRelMapper.listPlanPortfolioRel(userId, planId, channel);
    }

    @Override
    public List<PlanPortfolioModel> queryUserPlanPortfolioModel(String userId, Long planId) {
        return planPortfolioRelMapper.selectUserPlanPortfolioModel(userId,planId);
    }

    @Override
    public PlanPortfolioAccountDO queryUserAccountByPlanPortfolioRelId(Long planPortfolioRelId) {
        return portfolioAccountMapper.selectByPlanPortfolioRelId(planPortfolioRelId);
    }

    @Override
    public int updatePlanPortfolioAccount(PlanPortfolioAccountDO planPortfolioAccount) {
        return portfolioAccountMapper.update(planPortfolioAccount);
    }

    @Override
    public List<PlanPortfolioRelDO> queryPlanPortfolioRelList(FundChannelEnum channelEnum) {
        return planPortfolioRelMapper.selectByChannel(channelEnum.getChannel());
    }

    @Override
    public FundIncomeSyncDO queryFundIncomeSyncByDate(String userId, Date syncDate) {
        return null;
    }

    @Override
    public PlanInfoDO getPlanInfo(Long planInfoId) {
        return planInfoMapper.getByPlanInfoId(planInfoId);
    }

    @Override
    public PlanInfoDO queryPlanByThirdPlanId(String thirdPlanId, FundChannelEnum channel) {
        return planInfoMapper.getByThirdPlanId(thirdPlanId,channel.getChannel());
    }

    /**
     * 从db查询组合账户信息
     * @param planPortfolioRelId
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public PlanPortfolioAccountDO queryPortfolioAccountFromDB(Long planPortfolioRelId) {
        return portfolioAccountMapper.selectByPlanPortfolioRelId(planPortfolioRelId);
    }

    @Override
    public List<PlanInfoDO> listPlanInfo(String userId) {
        return planInfoMapper.listPlanInfo(userId);
    }

    @Override
    public int updatePlanNextRunDate(Long planInfoId, Date nextRunDate) {
        return planInfoMapper.updateNextRunDate(planInfoId,nextRunDate);
    }

    @Override
    public int updatePlanThirdPortfolioId(String userId, Long planInfoId, String portfolioId, FundChannelEnum channel) throws Exception {

        List<PlanPortfolioRelDO> planPortfolioRelDOS = planPortfolioRelMapper.listPlanPortfolioRel(userId,planInfoId,channel.getChannel());

        Preconditions.checkState(CollectionUtils.isNotEmpty(planPortfolioRelDOS),"查询用户:%s计划:%s组合关系失败",userId,planInfoId);

        PlanPortfolioRelDO planPortfolioRelDO = planPortfolioRelDOS.get(0);

        if (StringUtils.isBlank(planPortfolioRelDO.getThirdPortfolioId())) {
            //更新
           return planPortfolioRelMapper.updateThirdPortfolioId(planPortfolioRelDO.getPlanPortfolioRelId(),portfolioId);
        }

        return 0;
    }

    @Event(reliability = false,
            eventType = "",
            eventId = "",
            queue = "",
            exchange = "exchange.plan.nextrunremind",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void planNextRunRemind(PlanInfoDO planInfoDO) throws Exception {
        PlanNextRunRemindMessage message = new PlanNextRunRemindMessage();
        message.setUserId(planInfoDO.getUserId());
        message.setPlanInfoId(planInfoDO.getPlanInfoId());
        message.setPlanName(planInfoDO.getPlanName());
        message.setNextRunDate(planInfoDO.getNextRunDate());
        message.setPortfolioAmount(planInfoDO.getPortfolioAmount());
        EventMessageContext.addMessage(message);
    }

    /**
     * 获取计划收益
     * @param userId
     * @param planPortfolioRelId
     * @author yunpeng.zhang
     */
    @Override
    public PlanIncomeBO getPlanIncome(String userId, Long planPortfolioRelId) {
        PlanIncomeBO result = new PlanIncomeBO();
        PortfolioDailyIncomeDO portfolioDailyIncomeDO = portfolioDailyIncomeMapper.selectByUpdateDateAndShowedDate(userId, planPortfolioRelId);
        if (portfolioDailyIncomeDO != null) {
            result.setTotalIncome(portfolioDailyIncomeDO.getAccumulatedProfitloss());
        }
        return result;
    }

    @Override
    public PlanPortfolioRelDO queryUserPortfolioRelByPortfolioId(String userId, String thirdPortfolioId, FundChannelEnum channel) {
        return planPortfolioRelMapper.selectByPortfolioId(userId,thirdPortfolioId,channel.getChannel());
    }

    @Event(reliability = true,
            eventType = "'planExecuteRecordSyncComplete'",
            eventId = "#message.getEventId()",
            queue = "",
            exchange = "exchange.plan.executeRecord.syncComplete",
            version = "",
            amqpTemplate = "amqpTemplate"
    )
    @Override
    public void savePlanExecuteRecord(PlanInfoDO planInfoDO, List<YfPlanExecuteRecord> executeRecordList, String accountNumber) throws Exception {

        List<PlanExecuteRecordDO> planExecuteRecordDOList = Lists.newArrayList();

        for (YfPlanExecuteRecord yfPlanExecuteRecord : executeRecordList) {

            String merchantNumber = yfPlanExecuteRecord.getMerchantNumber();

            PlanExecuteRecordDO executeRecordDO = planExecuteRecordMapper.selectByMerchantNumber(merchantNumber);

            if (Objects.isNull(executeRecordDO)) {

                PlanExecuteRecordDO planExecuteRecordDO = new PlanExecuteRecordDO();
                planExecuteRecordDO.setUserId(planInfoDO.getUserId());
                planExecuteRecordDO.setPlanInfoId(planInfoDO.getPlanInfoId());
                planExecuteRecordDO.setAccountNumber(accountNumber);
                planExecuteRecordDO.setThirdPlanId(planInfoDO.getThirdPlanId());
                planExecuteRecordDO.setMerchantNumber(merchantNumber);
                planExecuteRecordDO.setPlanExecuteRecordId(guidCreater.getUniqueID());
                //执行时间
                if (CollectionUtils.isNotEmpty(yfPlanExecuteRecord.getProducts())) {
                    //取其中一个基金记录的下单时间
                    YfPlanExecuteFundDetail yfPlanExecuteFundDetail = yfPlanExecuteRecord.getProducts().get(0);
                    if (StringUtils.isNotBlank(yfPlanExecuteFundDetail.getOrderDate())) {
                        planExecuteRecordDO.setExecuteTime(new DateTime(Long.valueOf(yfPlanExecuteFundDetail.getOrderDate())).toDate());
                    }
                }

                planExecuteRecordDOList.add(planExecuteRecordDO);

            }
        }
        if (CollectionUtils.isNotEmpty(planExecuteRecordDOList)) {
            planExecuteRecordMapper.insertBatch(planExecuteRecordDOList);
        }

        PlanExecuteRecordSyncMessage message = new PlanExecuteRecordSyncMessage();
        message.setUserId(planInfoDO.getUserId());
        message.setPlanInfoId(planInfoDO.getPlanInfoId());
        message.setThirdPlanId(planInfoDO.getThirdPlanId());
        message.setAccountNumber(accountNumber);
        message.setEventId(planInfoDO.getPlanInfoId()+String.valueOf(new Date().getTime()));

        EventMessageContext.addMessage(message);

    }

    @Override
    public PlanExecuteRecordDO queryUserLastestExecuteRecord(String userId, Long planInfoId) {
        return planExecuteRecordMapper.selectByUserIdAndPlanInfoId(userId, planInfoId);
    }

    @Override
    public PlanInfoDO queryUserSuspenPlan(String userId) {
        return planInfoMapper.getSysSuspendPlanInfo(userId);
    }

    @Override
    public List<PlanPortfolioModel> queryUserPlanPortfolioModel(String userId) {
        return planPortfolioRelMapper.selectPlanPortfolioModelByUserId(userId);
    }

    @Override
    public List<PlanInfoDO> pagePlanInfo(Integer pageNo, Integer pageSize) {
        if (pageNo==null || pageNo<0){
            pageNo=0;
        }
        return planInfoMapper.pagePlanInfo(pageNo*pageSize,pageSize);
    }

    @Override
    public List<PlanInfoDO> pageAvailablePlanBeforeRunDate(Date nextRunDate, Integer pageNo, Integer pageSize) {
        if (pageNo==null || pageNo<0){
            pageNo=0;
        }
        return planInfoMapper.pageAvailablePlanBeforeRunDate(nextRunDate,pageNo*pageSize,pageSize);
    }
}
