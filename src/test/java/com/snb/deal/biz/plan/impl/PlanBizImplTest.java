package com.snb.deal.biz.plan.impl;

import com.snb.BaseBeanTest;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AccountSyncMessage;
import com.snb.deal.api.dto.insurance.InsuranceUploadRequest;
import com.snb.deal.api.dto.plan.*;
import com.snb.deal.api.enums.plan.PlanCycleEnum;
import com.snb.deal.api.remote.insurance.InsuranceRemote;
import com.snb.deal.biz.plan.PlanBiz;
import com.snb.deal.entity.plan.PlanInfoDO;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

public class PlanBizImplTest extends BaseBeanTest {

    @Resource
    PlanBiz planBiz;

    @Resource
    InsuranceRemote insuranceRemote;

    @Test
    public void uploaduser(){
        InsuranceUploadRequest req = new InsuranceUploadRequest();
        req.setCurrentUserId("d9a510b3acd34be0a687d6d2b287b61e");
        req.setInsuranceUserIdCard("130102198510282145");
        req.setInsuranceUserName("王洋");
        req.setInsuranceUserPhone("13699264700");
        APIResponse response = insuranceRemote.uploadUserInsurance(req);
        System.out.println(response);
    }
    @Test
    public void createPlan() throws Exception {

        CreatePlanRequest request = new CreatePlanRequest();
        request.setUserId("941325cc68ac4952bcf8f7db6a8e09b9");
        request.setCycleDay(25);
        request.setFundUserAccountId(1);
        request.setFundUserAccount("JLC20180330000026962");
        request.setPortfolioAmount(new BigDecimal(100000));
        request.setPlanName("A");
        request.setInvestorPayId("10763");

        planBiz.createPlan(request);


    }
    @Test
    public void modifyPlan() throws Exception {
        ModifyPlanRequest request = new ModifyPlanRequest();
        request.setUserId("941325cc68ac4952bcf8f7db6a8e09b9");
        request.setPlanId(40226511208512L);
        request.setCycle(PlanCycleEnum.MONTYLY);
        request.setCycleDay(13);
        request.setFundUserAccount("JLC20180330000026962");
        request.setFundUserAccountId(1);
        request.setPortfolioAmount(new BigDecimal(3000));
        planBiz.modifyPlan(request);
    }
    @Test
    public void suspendPlan() throws Exception {
        SuspendPlanRequest request = new SuspendPlanRequest();
        request.setUserId("941325cc68ac4952bcf8f7db6a8e09b9");
        request.setPlanId(40226511208512L);
        request.setFundUserAccount("JLC20180330000026962");
        request.setFundUserAccountId(1);

        planBiz.suspendPlan(request);
    }

    @Test
    public void restartPlan() throws Exception {
        RestartPlanRequest request = new RestartPlanRequest();
        request.setUserId("941325cc68ac4952bcf8f7db6a8e09b9");
        request.setPlanId(40226511208512L);
        request.setFundUserAccount("JLC20180330000026962");
        request.setFundUserAccountId(1);
        planBiz.restartPlan(request);
    }

    @Test
    public void updatePlanNextRunDate() throws Exception {
        planBiz.updatePlanNextRunDate("23801",null, FundChannelEnum.YIFENG);
    }

    @Test
    public void testSyncAccount() throws Exception {
        AccountSyncMessage message = new AccountSyncMessage();
        message.setUserId("fa30a92b6df24acf8051548583395491");
        message.setAccountNumber("JLC20180510000027805");
        planBiz.syncPortfolioAccount(message);
    }

    @Test
    public void getPlanAssetDetail() throws Exception {
        planBiz.getPlanAssetDetail("a0859604581f4f17977f038866123fa3", 85163224735744L);
    }

    @Test
    public void testSyncPlanExecuteRecord() throws Exception {
        PlanInfoDO planInfoDO = new PlanInfoDO();
        planInfoDO.setUserId("8752a1ea6f95482f9e6550390edf1cea");
        planInfoDO.setThirdPlanId("23843");
        planInfoDO.setPlanInfoId(46234184425472L);
        planBiz.syncPlanExecuteRecord(planInfoDO);
    }

    @Test
    public void getPlanAssetInfo() throws Exception {
        PlanAssetInfoResponse planAssetInfoResponse = planBiz.getPlanAssetInfo("f6f82b76017442cb838665dd6c8b8d74", 48722657042432L, 1);
        System.out.println(planAssetInfoResponse);
    }
    @Test
    public void testSyncPlanInfo() throws Exception {
        PlanInfoDO planInfoDO = new PlanInfoDO();
        planInfoDO.setPlanInfoId(47403330531392L);
        planInfoDO.setPlanStatus(2);
        planInfoDO.setUserId("d9a510b3acd34be0a687d6d2b287b61e");
        planInfoDO.setThirdPlanId("24155");
        planBiz.syncPlanInfo(planInfoDO);
    }


    @Test
    public void getPlanAutoInvestInfo() throws Exception{
        PlanAutoInvestRequest param = new PlanAutoInvestRequest();
        param.setUserId("d9a510b3acd34be0a687d6d2b287b61e");
        PlanAutoInvestResponse response = planBiz.getPlanAutoInvestInfo(param);
        System.out.println(response);

    }

}