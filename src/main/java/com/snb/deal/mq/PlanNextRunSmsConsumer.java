package com.snb.deal.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.common.collect.Maps;
import com.jianlc.event.EventMessageHandlerAdaptor;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.SmsConfTypeEnum;
import com.snb.common.mq.bean.PlanNextRunRemindMessage;
import com.snb.common.mq.bean.SmsMessage;
import com.snb.common.mq.enums.Exchange;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.dto.user.UserInfoResponse;
import com.snb.user.remote.FundUserRemote;
import com.snb.user.remote.UserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * 计划下次执行提醒-发送短信
 * 【拾年保】亲爱的李女士，4月1日是您的”拾年保“定投计划扣款日，请确保银行卡内资金充足。每一分的投入都是对未来的加倍保障
 */
@Slf4j
public class PlanNextRunSmsConsumer extends EventMessageHandlerAdaptor<PlanNextRunRemindMessage> {

    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Reference(version = "1.0")
    private UserRemote userRemote;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    protected void handleData(PlanNextRunRemindMessage message) throws Exception {

        log.info("收到计划下次执行提醒发送短信消息：{}",message);

        if (Objects.isNull(message)) {
            return;
        }

        if (StringUtils.isBlank(message.getUserId()) || Objects.isNull(message.getNextRunDate())) {
            return;
        }

        String userId = message.getUserId();
        Date nextRunDate = message.getNextRunDate();

        //查询手机号
        APIResponse<UserInfoResponse> apiResponse = userRemote.getUserInfo(userId);

        if (Objects.isNull(apiResponse) || !apiResponse.isSuccess()
                || Objects.isNull(apiResponse.getData())) {
            log.error("查询用户：{}信息失败",userId);
            return;
        }
        String mobile = apiResponse.getData().getPhone();

        //获取用户基金账户相关信息
        APIResponse<GetUserFundAccountInfoResponse> fundUserResponse = null;
        try {
            BaseFundRequest req = new BaseFundRequest();
            req.setUserId(userId);
            fundUserResponse = fundUserRemote.getUserFundAccountInfo(req);
        } catch (Exception e) {
            log.error("获取用户：{}基金账户信息异常",userId,e);
            return;
        }
        if (Objects.isNull(fundUserResponse)
                || !fundUserResponse.isSuccess() || Objects.isNull(fundUserResponse.getData())) {
            log.error("查询用户：{}基金账户信息失败",userId);
            return;
        }

        String userName = fundUserResponse.getData().getIdentityName();
        Integer gender = fundUserResponse.getData().getGender();
        String nickName = gender==1?"先生":"女士";
        nickName=userName.substring(0,1)+nickName;
        String runDate = new DateTime(nextRunDate).toString("M月d日");

        Map<String,String> param = Maps.newHashMap();
        param.put("nickName",nickName);
        param.put("nextRunDate",runDate);
        SmsMessage smsMessage = SmsMessage.builder()
                .mobile(mobile)
                .confType(SmsConfTypeEnum.PLAN_NEXTRUN_REMIND)
                .param(param).build();

        amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(),smsMessage);

        log.info("用户：{}计划提醒短信发送完成：{}",userId,smsMessage);
    }

}
