package com.snb.remote;

import com.snb.BaseBeanTest;
import com.snb.common.dto.APIResponse;
import com.snb.deal.api.dto.plan.PlanAutoInvestRequest;
import com.snb.deal.api.dto.plan.PlanAutoInvestResponse;
import com.snb.deal.api.remote.plan.PlanRemote;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * USER:    zhangwenting
 * TIME:    2018-08-01 10:23
 * COMMENT:
 */
public class TestPlanRemote extends BaseBeanTest {
    @Resource
    private PlanRemote remote;

    @Test
    public void testPlan() {
        PlanAutoInvestRequest param = new PlanAutoInvestRequest();
        param.setUserId("dfc920078f3b47e4993f9d6339e2ca36");
        param.setPageNo(1);
        param.setPageSize(5);
        APIResponse<PlanAutoInvestResponse> response =  remote.getPlanAutoInvestInfo(param);
        System.out.println(response);
    }

    @Test
    public void test() {
         remote.getPlanInfoListByUserId("89a54bbff52241fb9fc9037d7fd3952f");
    }
}
