package com.snb.deal.remote.admin.insurance;

import com.alibaba.dubbo.config.annotation.Service;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.admin.api.dto.insurance.InsuranceDTO;
import com.snb.deal.admin.api.dto.insurance.InsuranceParam;
import com.snb.deal.admin.api.dto.insurance.InsuranceRequest;
import com.snb.deal.admin.api.dto.insurance.InsuranceResultDto;
import com.snb.deal.admin.api.remote.insurance.InsuranceAdminRemote;
import com.snb.deal.entity.insurance.InsuranceDO;
import com.snb.deal.service.insurance.InsuranceService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service(version = "1.0")
public class InsuranceAdminRemoteImpl implements InsuranceAdminRemote {
    @Resource
    InsuranceService insuranceService;
    public APIResponse updateInsurance(InsuranceRequest param){
        try{
            insuranceService.updateInsurance(param);
        }catch (Exception e){
            log.error("更新保险相关信息失败 param={}",param,e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }

        return APIResponse.build(SystemResultCode.SUCCESS);
    }

    @Override
    public APIResponse<List<InsuranceResultDto>> queryInsuranceByTime(InsuranceParam param) {
        try{
            List<InsuranceResultDto> insuranceResultList = insuranceService.getInsuranceByTime(param);
            return APIResponse.build(SystemResultCode.SUCCESS,insuranceResultList);
        }catch (Exception e){
            e.printStackTrace();
            log.error("根据时间段查询保险信息失败 param={}",param,e);
            return APIResponse.build(SystemResultCode.SYSTEM_ERROR);
        }
    }
}
