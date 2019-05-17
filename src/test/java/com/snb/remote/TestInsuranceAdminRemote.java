package com.snb.remote;

import com.snb.BaseBeanTest;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.common.datetime.DateUtil;
import com.snb.common.dto.APIResponse;
import com.snb.deal.admin.api.dto.insurance.InsuranceParam;
import com.snb.deal.admin.api.dto.insurance.InsuranceResultDto;
import com.snb.deal.admin.api.remote.insurance.InsuranceAdminRemote;
import com.snb.deal.api.dto.insurance.InsuranceRequest;
import com.snb.deal.api.dto.insurance.InsuranceResponse;
import com.snb.deal.api.dto.invest.InvestRequest;
import com.snb.deal.api.remote.insurance.InsuranceRemote;
import com.snb.deal.api.remote.order.OrderInvestRemote;
import com.snb.deal.entity.insurance.InsuranceDO;
import com.snb.user.util.JSONUtil;
import org.junit.Test;

import javax.annotation.Resource;
import javax.xml.crypto.Data;
import java.util.Date;

/**
 * USER:    xiepengfei
 * TIME:    2018-07-26 11:23
 * COMMENT:
 */
public class TestInsuranceAdminRemote extends BaseBeanTest{
    @Resource
    private InsuranceAdminRemote remote;
    @Resource
    private InsuranceRemote insuranceRemote;

    @Test
    public void testQueryInsurance() {
//        InsuranceParam insuranceParam = new InsuranceParam();
//        insuranceParam.setStartTime(DateUtil.addDays(new Date(),-120));
//        insuranceParam.setEndTime(new Date());
//        APIResponse<InsuranceResultDto> apiResponse = remote.queryInsuranceByTime(insuranceParam);
    }
    @Test
    public void testinsorance(){
        try {
            String userId= "2cd1f265b7ca4be782fc0b4194246ac7";
//            InsuranceResponse remotesult = new InsuranceResponse();
            InsuranceRequest insuranceRequest = new InsuranceRequest();
            insuranceRequest.setUserId(userId);
            insuranceRemote.getInsurance(insuranceRequest);
        }catch (Exception e){
            System.out.println("报错");
        }
    }

}
