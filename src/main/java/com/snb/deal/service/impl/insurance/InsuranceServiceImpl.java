package com.snb.deal.service.impl.insurance;

import com.jianlc.tc.guid.GuidCreater;
import com.snb.deal.admin.api.dto.insurance.InsuranceParam;
import com.snb.deal.admin.api.dto.insurance.InsuranceRequest;
import com.snb.deal.admin.api.dto.insurance.InsuranceResultDto;
import com.snb.deal.api.dto.insurance.InsuranceUploadRequest;
import com.snb.deal.entity.insurance.InsuranceDO;
import com.snb.deal.enums.InsuranceStatusEnum;
import com.snb.deal.mapper.insurance.InsuranceMapper;
import com.snb.deal.service.insurance.InsuranceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class InsuranceServiceImpl implements InsuranceService {

    @Autowired
    private InsuranceMapper insuranceMapper;

    @Autowired
    private GuidCreater guidCreater;

    @Autowired
    private Environment environment;

    @Override
    public InsuranceDO getInsurance(String userId, Long insuranceId) {
        return insuranceMapper.selectByIdOrUserId(insuranceId, userId);
    }

    @Override
    public List<InsuranceDO> getUserInsurances(String userId) {
        return insuranceMapper.selectByUserId(userId);
    }

    @Override
    public int saveInsurance(InsuranceDO insuranceDO) {
        return insuranceMapper.insert(insuranceDO);
    }

    @Override
    public int updateInsurance(InsuranceRequest request) {
        return insuranceMapper.update(request.getList());
    }

    @Override
    public int saveUploadUserInfo(InsuranceUploadRequest request){
        // 保险名称
        String insuranceName = environment.getProperty("insurance.name");
        // 保险购买金额
        String insuranceBuyAmount = environment.getProperty("insurance.buyAmount");
        // 保障期限
        String indemnificationTimeLimit = environment.getProperty("insurance.IndemnificationTimeLimit");
        // 保障金额
        String indemnificationAmount = environment.getProperty("insurance.IndemnificationAmount");

        InsuranceDO insuranceDO = new InsuranceDO();
        insuranceDO.setInsuranceId(guidCreater.getUniqueID());
        insuranceDO.setUserId(request.getCurrentUserId());
        insuranceDO.setBuyAmount(BigDecimal.ZERO);
        insuranceDO.setIndemnificationAmount(new BigDecimal(indemnificationAmount));
        insuranceDO.setIndemnificationTimeLimit(indemnificationTimeLimit);
        insuranceDO.setInsuredName(request.getInsuranceUserName());
        insuranceDO.setInsuredIdCard(request.getInsuranceUserIdCard());
        insuranceDO.setInsuranceNo("");
        insuranceDO.setInsuredPhone(request.getInsuranceUserPhone());
        insuranceDO.setInsuranceStatus(InsuranceStatusEnum.REVIEWING.getCode());
        insuranceDO.setInsuranceName(insuranceName);
        insuranceDO.setBuyAmount(new BigDecimal(insuranceBuyAmount));
        insuranceDO.setReason("");
        insuranceDO.setCreateTime(new Date());
        insuranceDO.setUpdateTime(new Date());
        insuranceDO.setYn(0);
        int result = -1;
        try{
            insuranceMapper.deleteInsurance(insuranceDO.getUserId());
            result = insuranceMapper.insert(insuranceDO);
        }catch(Exception e){
            log.error("保存用户上传保险信息出错 error = ",e);
        }
        return result;
    }

    @Override
    public int changeInsuranceStatus(String userId,int status){
        return insuranceMapper.updateInsuranceStatus(userId,status);
    }

    @Override
    public List<InsuranceResultDto> getInsuranceByTime(InsuranceParam param) {
        return insuranceMapper.selectInsuranceByTime(param);
    }
}
