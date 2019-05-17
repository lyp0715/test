package com.snb.deal.remote.insurance;

import com.jianlc.tc.guid.GuidCreater;
import com.snb.BaseBeanTest;
import com.snb.common.dto.APIResponse;
import com.snb.deal.api.dto.insurance.InsuranceRequest;
import com.snb.deal.api.dto.insurance.InsuranceResponse;
import com.snb.deal.api.dto.insurance.InsuranceUploadRequest;
import com.snb.deal.api.remote.insurance.InsuranceRemote;
import com.snb.deal.service.insurance.InsuranceService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

public class InsuranceRemoteImplTest extends BaseBeanTest{
    @Resource
    private InsuranceRemote insuranceRemote;
    @Resource
    private GuidCreater guidCreater;
    @Autowired
    private InsuranceService insuranceService;

    @Test
    public void getInsurance() {
        InsuranceRequest request = new InsuranceRequest();
        request.setUserId("9bb84923652e47e899583e5ae0069dc5");
        APIResponse<InsuranceResponse> insurance = insuranceRemote.getInsurance(request);
        System.out.println(insurance);
    }

    @Test
    public void generatorId() {
        long uniqueID = guidCreater.getUniqueID();
        System.out.println(uniqueID);
    }

    @Test
    public void uploadInsurance() {
        InsuranceUploadRequest request = new InsuranceUploadRequest();
        request.setCurrentUserId("d9a510b3acd34be0a687d6d2b287b61e");
        request.setInsuranceUserIdCard("1234567890");
        request.setInsuranceUserName("王洋");
        request.setInsuranceUserPhone("3333333333333");
        int saveHistoryResult = insuranceService.saveUploadUserInfo(request);
        System.out.println(saveHistoryResult);
    }
}