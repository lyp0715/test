package com.snb.deal.remote.insurance;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.api.dto.insurance.InsuranceRequest;
import com.snb.deal.api.dto.insurance.InsuranceResponse;
import com.snb.deal.api.dto.insurance.InsuranceUploadRequest;
import com.snb.deal.api.remote.insurance.InsuranceRemote;
import com.snb.deal.entity.insurance.InsuranceDO;
import com.snb.deal.enums.ResultCode;
import com.snb.deal.service.insurance.InsuranceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Slf4j
@Service(version = "1.0")
public class InsuranceRemoteImpl implements InsuranceRemote {

    @Autowired
    private InsuranceService insuranceService;

    /**
     * 获取保险信息，返回的数据可能为空
     *
     * @param request 包含计划id或保险id
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public APIResponse<InsuranceResponse> getInsurance(InsuranceRequest request) {
        log.info("获取保险信息，request：{}", JSON.toJSONString(request));

        //0. 参数校验
        if (request == null || StringUtils.isEmpty(request.getUserId())) {
            log.error("获取保险信息参数校验失败！，request：{}", JSON.toJSONString(request));
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }

        //1. 查询保险信息
        InsuranceDO insuranceDO;
        try {
            insuranceDO = insuranceService.getInsurance(request.getUserId(), request.getInsuranceId());
        } catch (Exception e) {
            log.error("查询保险信息异常！用户：{}，保险id：{}", request.getUserId(), request.getInsuranceId(), e);
            return APIResponse.build(ResultCode.PLAN_INSURANCE_ERROR);
        }

        //2.组织数据
        InsuranceResponse insuranceResponse = new InsuranceResponse();
        if (insuranceDO != null) {
            BeanUtils.copyProperties(insuranceDO, insuranceResponse);
            // 计算剩余保期
            Date deadlineDate = insuranceDO.getDeadlineDate();
            if (deadlineDate != null) {
                int surplusDays = Days.daysBetween(new DateTime(),new DateTime(deadlineDate)).getDays();
                surplusDays = surplusDays <= 0 ? 0 : surplusDays;
                insuranceResponse.setSurplusDays(surplusDays);
            } else {
                log.info("查询到保险截止日期为空！用户：{}，保险id：{}", request.getUserId(), request.getInsuranceId());
                insuranceResponse.setSurplusDays(0);
            }
        } else {
            log.info("查询到保险信息为空！用户：{}，保险id：{}", request.getUserId(), request.getInsuranceId());
        }

        return APIResponse.build(SystemResultCode.SUCCESS).setData(insuranceResponse);
    }

    @Override
    public APIResponse uploadUserInsurance(InsuranceUploadRequest request){
        log.info("开始上传保险信息");

        if (request == null || StringUtils.isEmpty(request.getCurrentUserId())) {
            log.error("获取用户上传保险信息参数校验失败！，request：{}", JSON.toJSONString(request));
            return APIResponse.build(SystemResultCode.PARAM_ERROR);
        }
        try {
            insuranceService.saveUploadUserInfo(request);
        } catch (Exception e) {
            log.error("保存用户保险信息异常！用户：{}", request.getCurrentUserId(), e);
            return APIResponse.build(ResultCode.PLAN_GET_UPLOAD_INSURANCE_ERROR);
        }
        return APIResponse.build(SystemResultCode.SUCCESS);
    }
}
