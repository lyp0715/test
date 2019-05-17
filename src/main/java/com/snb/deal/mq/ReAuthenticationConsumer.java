package com.snb.deal.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.jianlc.event.EventMessage;
import com.jianlc.event.ReliabilityEventMessageHandlerAdaptor;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.SmsConfTypeEnum;
import com.snb.common.mq.bean.ReAuthenticationMessage;
import com.snb.common.mq.bean.SmsMessage;
import com.snb.common.mq.enums.Exchange;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.dto.user.UserInfoResponse;
import com.snb.user.remote.FundUserRemote;
import com.snb.user.remote.UserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class ReAuthenticationConsumer {
    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Reference(version = "1.0")
    private UserRemote userRemote;

    protected void handleData(ReAuthenticationMessage reAuthenticationMessage) throws Exception {
        log.info("定投完成，处理用户重新鉴权开始：{}", JSON.toJSONString(reAuthenticationMessage));
        try {
            String userId = reAuthenticationMessage.getUserId();
//            BigDecimal investAmount = reAuthenticationMessage.getInvestAmount();
            APIResponse<Boolean> response;
            try {
                response = fundUserRemote.needReAuthentication(userId);
            } catch (Exception e) {
                log.error("定投完成，用户[{}]重新鉴权系统异常", userId, e);
                return;
            }

            //查询手机号
            APIResponse<UserInfoResponse> apiResponse = userRemote.getUserInfo(userId);

            if (Objects.isNull(apiResponse) || !apiResponse.isSuccess()
                    || Objects.isNull(apiResponse.getData())) {
                log.error("查询用户：{}信息失败", userId);
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
                log.error("获取用户：{}基金账户信息异常", userId, e);
                return;
            }
            if (Objects.isNull(fundUserResponse)
                    || !fundUserResponse.isSuccess() || Objects.isNull(fundUserResponse.getData())) {
                log.error("查询用户：{}基金账户信息失败", userId);
                return;
            }

            Integer gender = fundUserResponse.getData().getGender();
            //获取用户姓氏
            String firstName = getFirstname(fundUserResponse.getData().getIdentityName());


            String nickName = gender == 1 ? "先生" : "女士";

            if (response.isSuccess()) {
                Boolean result = response.getData();
                log.info("定投完成，用户[{}]需要重新鉴权", userId);
                // 对于需要重新鉴权的用户，下发短信
                if (result) {
                    Map<String, String> param = Maps.newHashMap();
                    param.put("nickName", firstName + nickName);
                    SmsMessage smsMessage = SmsMessage.builder().mobile(mobile).confType(SmsConfTypeEnum.AUTO_INVEST_RE_AUTHENTICATION).param(param).build();
                    amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(), smsMessage);
                }
            }
        } catch (Exception e) {
            log.error("定投完成，处理用户重新鉴权异常", e);
        }
    }

    /**
     * 获取用户姓氏
     * @param identityName
     * @return
     */
    private static String getFirstname(String identityName) {
        if (StringUtils.isNotBlank(identityName)) {
            identityName = identityName.trim();
            if (4 == identityName.length()) {
                return identityName.substring(0, 2);
            } else {
                return identityName.substring(0, 1);
            }
        }
        return StringUtils.EMPTY;
    }

}
