package com.snb.deal.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.jianlc.tc.guid.GuidCreater;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.PlanCreatedMessage;
import com.snb.deal.entity.insurance.InsuranceDO;
import com.snb.deal.enums.InsuranceStatusEnum;
import com.snb.deal.service.insurance.InsuranceService;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.remote.FundUserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 计划创建完成，保险下单
 */
@Slf4j
public class InitInsuranceConsumer extends ReliabilityEventMessageHandlerAdaptor<PlanCreatedMessage> {

    @Autowired
    private InsuranceService insuranceService;
    @Autowired
    private GuidCreater guidCreater;
    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Autowired
    private Environment environment;

    @Override
    protected void handleData(PlanCreatedMessage message) throws Exception {

        log.info("计划创建完成，初始化保险信息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        String userId = message.getUserId();
        Long planId = message.getPlanId();

        if (StringUtils.isEmpty(userId) || Objects.isNull(planId)) {
            return;
        }

        BaseFundRequest request = new BaseFundRequest();
        request.setUserId(userId);
        request.setFundPlatform(FundChannelEnum.YIFENG);
        APIResponse<GetUserFundAccountInfoResponse> userFundAccountInfo = fundUserRemote.getUserFundAccountInfo(request);
        if (!userFundAccountInfo.isSuccess()) {
            log.error("获取用户基金账户失败，" + userFundAccountInfo.getMsg());
            return;
        }

        GetUserFundAccountInfoResponse data = userFundAccountInfo.getData();
        if (data == null) {
            log.error("获取到用户基金账户为空！");
            return;
        }

        List<InsuranceDO> insuranceDOList = insuranceService.getUserInsurances(userId);

        if (CollectionUtils.isEmpty(insuranceDOList)) {
            // 保险名称
            String insuranceName = environment.getProperty("insurance.name");
            // 保险购买金额
            String insuranceBuyAmount = environment.getProperty("insurance.buyAmount");
            // 保障期限
            String indemnificationTimeLimit = environment.getProperty("insurance.IndemnificationTimeLimit");
            // 保障金额
            String indemnificationAmount = environment.getProperty("insurance.IndemnificationAmount");

            //保存用户保险信息
            InsuranceDO insuranceDO = new InsuranceDO();
            insuranceDO.setInsuranceId(guidCreater.getUniqueID());
            insuranceDO.setUserId(userId);
            insuranceDO.setBuyAmount(BigDecimal.ZERO);
            insuranceDO.setIndemnificationAmount(new BigDecimal(indemnificationAmount));
            insuranceDO.setIndemnificationTimeLimit(indemnificationTimeLimit);
            if (StringUtils.isNotEmpty(data.getIdentityName())) {
                insuranceDO.setInsuredName(data.getIdentityName());
            }
            insuranceDO.setInsuranceStatus(InsuranceStatusEnum.UNCLAIMED.getCode());
            insuranceDO.setInsuredIdCard(data.getIdentityNumber());
            insuranceDO.setInsuredPhone("");
            insuranceDO.setInsuranceName(insuranceName);
            insuranceDO.setBuyAmount(new BigDecimal(insuranceBuyAmount));

            insuranceDO.setCreateTime(new Date());
            insuranceDO.setUpdateTime(new Date());
            insuranceDO.setReason("");
            insuranceDO.setYn(0);

            insuranceService.saveInsurance(insuranceDO);
        }
    }
}
