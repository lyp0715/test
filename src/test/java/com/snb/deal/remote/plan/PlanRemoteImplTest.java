package com.snb.deal.remote.plan;

import com.snb.BaseBeanTest;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.api.dto.plan.*;
import com.snb.deal.api.remote.plan.PlanRemote;
import org.junit.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;

public class PlanRemoteImplTest extends BaseBeanTest {

    @Resource
    private PlanRemote planRemote;


    @Test
    public void createPlan() {
    }

    @Test
    public void planDetail1() {
        PlanDetailRequest request = new PlanDetailRequest();
        request.setPlanInfoId(45156486144000L);
        request.setUserId("fa30a92b6df24acf8051548583395491");
        APIResponse<PlanDetailResponse> apiResponse = planRemote.planDetail(request);
        if (apiResponse.isSuccess()) {
            System.out.println(apiResponse.getData());
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void modifyPlan() {
        ModifyPlanRequest request  = new ModifyPlanRequest();
        request.setPlanId(65644014161984L);
//        request.setPlanId(28355L);
        request.setUserId("82218f164aa54739b4c539b181da21cd");
        request.setCycleDay(11);
        request.setPortfolioAmount(new BigDecimal(6000));
        request.setFundUserAccount("JLC20180705000028914");
        request.setFundUserAccountId(65643715842112L);
        request.setInvestorPayId("11786");
        request.setPortfolioYear(0);
        planRemote.modifyPlan(request);
    }

    @Test
    public void suspendPlan() {
    }

    @Test
    public void restartPlan() {
    }

    @Test
    public void getPlanPortfolioRelList() {
        PlanPortfolioRelRequest request = new PlanPortfolioRelRequest();
        request.setPlanInfoId(40940223459520L);
        request.setUserId("bf49bc0167c74067872405a9ff4cd398");
        request.setChannel(FundChannelEnum.YIFENG.getChannel());
        APIResponse<PlanPortfolioRelResponse> planPortfolioRelList = planRemote.getPlanPortfolioRelList(request);
        if (planPortfolioRelList.isSuccess()) {
            System.out.println(planPortfolioRelList);
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getPlanPortfolioAccount() {
        APIResponse<PlanPortfolioAccountResponse> planPortfolioAccount = planRemote.getPlanPortfolioAccount(40221190754368L);
        if (planPortfolioAccount.isSuccess()) {
            System.out.println(planPortfolioAccount);
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getPlanInfo() {
        APIResponse<PlanInfoResponse> planInfo = planRemote.getPlanInfo(40221190754368L);
        if (planInfo.isSuccess()) {
            System.out.println(planInfo.getData());
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getPlanAssetDetail() {
        PlanAssetDetailRequest request = new PlanAssetDetailRequest();
        request.setUserId("fa30a92b6df24acf8051548583395491");
        request.setPlanId(45156486144000L);
        APIResponse<PlanAssetDetailResponse> planAssetDetail = planRemote.getPlanAssetDetail(request);
        if (planAssetDetail.isSuccess()) {
            System.out.println(planAssetDetail);
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getPlanAssetInfo() {
        PlanAssetInfoRequest request = new PlanAssetInfoRequest();
        request.setUserId("ba8b203998014664a6641a908f73e8f7");
        request.setPlanId(44071690481664L);
        APIResponse<PlanAssetInfoResponse> planAssetInfo = planRemote.getPlanAssetInfo(request);
        if (planAssetInfo.isSuccess()) {
            System.out.println(planAssetInfo);
        } else {
            System.out.println("失败！");
        }
    }

    @Test
    public void getPlanIncomeInfo() {
        PlanIncomeInfoRequest request = new PlanIncomeInfoRequest();
        request.setUserId("fa30a92b6df24acf8051548583395491");
        request.setPlanId(45156486144000L);
        APIResponse<PlanIncomeInfoResponse> planIncomeInfo = planRemote.getPlanIncomeInfo(request);
        System.out.println(planIncomeInfo);
    }
}