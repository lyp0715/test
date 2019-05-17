package com.snb.deal.service.insurance;

import com.snb.deal.admin.api.dto.insurance.InsuranceParam;
import com.snb.deal.admin.api.dto.insurance.InsuranceRequest;
import com.snb.deal.admin.api.dto.insurance.InsuranceResultDto;
import com.snb.deal.api.dto.insurance.InsuranceUploadRequest;
import com.snb.deal.entity.insurance.InsuranceDO;

import java.util.List;

public interface InsuranceService {
    /**
     * 根据用户id和计划id查询保险信息
     *
     * @param userId 用户id
     * @param insuranceId 保险id
     * @return
     * @author yunpeng.zhang
     */
    InsuranceDO getInsurance(String userId, Long insuranceId);

    /**
     *  获取用户保险信息
     * @param userId
     * @return
     */
    List<InsuranceDO> getUserInsurances(String userId);

    /**
     * 保存用户保险信息
     * @param insuranceDO
     * @return
     */
    int saveInsurance(InsuranceDO insuranceDO);

    /**
     * 批量更新用户保险信息
     * @param request
     * @return
     */
    int updateInsurance(InsuranceRequest request);

    /**
     * 保存用户上传的信息
     * @param request
     * @return
     */
    int saveUploadUserInfo(InsuranceUploadRequest request);

    /**
     * 更改用户保单状态
     * @param userId 用户id,
     * @param status 保险状态
     * @return
     */
    int changeInsuranceStatus(String userId,int status);

    /**
     * 根据时间段获取保险信息
     * @param param
     * @return
     */
    List<InsuranceResultDto> getInsuranceByTime(InsuranceParam param);
}
