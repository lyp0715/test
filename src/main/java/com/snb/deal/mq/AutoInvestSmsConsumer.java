package com.snb.deal.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.common.collect.Maps;
import com.jianlc.event.EventMessageHandlerAdaptor;
import com.snb.common.dto.APIResponse;
import com.snb.common.enums.SmsConfTypeEnum;
import com.snb.common.mq.bean.InvestOrderSyncCompleteMessage;
import com.snb.common.mq.bean.SmsMessage;
import com.snb.common.mq.enums.Exchange;
import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.api.enums.order.InvestTypeEnum;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.enums.TransactionStatusEnum;
import com.snb.deal.service.order.OrderInfoService;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.user.dto.fund.BaseFundRequest;
import com.snb.user.dto.fund.GetUserFundAccountInfoResponse;
import com.snb.user.dto.user.UserInfoResponse;
import com.snb.user.remote.FundUserRemote;
import com.snb.user.remote.UserRemote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 定投订单完成扣款-短信通知
 */
@Slf4j
public class AutoInvestSmsConsumer extends EventMessageHandlerAdaptor<InvestOrderSyncCompleteMessage> {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private OrderInvestService orderInvestService;
    @Resource
    private OrderInfoService orderInfoService;
    @Reference(version = "1.0")
    private FundUserRemote fundUserRemote;
    @Reference(version = "1.0")
    private UserRemote userRemote;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    protected void handleData(InvestOrderSyncCompleteMessage message) throws Exception {

        log.info("收到定投订单完成扣款消息：{}",message);

        try {

            if (Objects.isNull(message)) {
                return;
            }

            //仅处理定投订单
            if (Objects.isNull(message.getInvestType())
                    || !String.valueOf(message.getInvestType()).equals(String.valueOf(InvestTypeEnum.AUTO_INVEST.getType()))) {
                return;
            }
            String userId = message.getUserId();
            Long orderNo = message.getOrderNo();
            Long orderInvestId = message.getOrderInvestId();

            //因为发送短信时，此订单状态是处于中间状态，所以会出现消息重复发送，需要判断此订单是否已经发过短信通知了
            //把userId和订单号存入redis，设置失效日期为1个月
            String key = userId+orderNo;
            if (redisTemplate.hasKey(key)) {
                log.debug("用户：{}订单：{}已经发送短信通知",userId,orderNo);
                return;
            }

            //查询订单
            OrderInfoDO orderInfoDO =orderInfoService.queryByOrderNo(orderNo);
            if (Objects.isNull(orderInfoDO)) {
                log.error("查询订单：{}失败",orderNo);
                return;
            }
            //查询订单详情
            OrderInvestDetailRequest condition = new OrderInvestDetailRequest();
            condition.setOrderInvestId(orderInvestId);
            List<OrderInvestDetailDO> orderInvestDetailDOList = orderInvestService.listOrderInvestDetail(condition);
            if (CollectionUtils.isEmpty(orderInvestDetailDOList)) {
                log.error("没有查询到订单详情：{}",orderInvestId);
                return;
            }
            //付款成功金额和失败金额
            BigDecimal successAmount = BigDecimal.ZERO,failedAmount = BigDecimal.ZERO;
            for (OrderInvestDetailDO orderInvestDetailDO : orderInvestDetailDOList) {

                //如果订单中存在未付款订单，则不发送短信通知
                if (orderInvestDetailDO.getTransactionStatus() == TransactionStatusEnum.PAYING.getCode() ||
                        orderInvestDetailDO.getTransactionStatus() ==  TransactionStatusEnum.WAIT_PAY.getCode()) {
                    return;
                }
                if (orderInvestDetailDO.getTransactionStatus() == TransactionStatusEnum.PAY_FAIL.getCode()) {
                    //支付失败
                    failedAmount=failedAmount.add(orderInvestDetailDO.getTransactionAmount());
                } else {
                    //其他状态都是扣款成功的
                    successAmount=successAmount.add(orderInvestDetailDO.getTransactionAmount());
                }
            }
            if (successAmount.compareTo(BigDecimal.ZERO) == 0
                    && failedAmount.compareTo(BigDecimal.ZERO) == 0) {
                log.error("查询到订单：{}明细支付成功金额和支付失败金额都是0",orderInvestId);
                return;
            }
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
            SmsConfTypeEnum confType = SmsConfTypeEnum.AUTO_INVEST_ALL_PAY_SUCCESS;
            Map<String,String> param = Maps.newHashMap();
            param.put("nickName",nickName);
            if (successAmount.compareTo(BigDecimal.ZERO) == 0) {
                //全部失败
                confType = SmsConfTypeEnum.AUTO_INVEST_ALL_PAY_FAILED;
                param.put("failedAmount",failedAmount.toString());
            }else if (failedAmount.compareTo(BigDecimal.ZERO) == 0) {
                //全部成功
                confType = SmsConfTypeEnum.AUTO_INVEST_ALL_PAY_SUCCESS;
                param.put("successAmount",successAmount.toString());
            } else {
                //部分成功
                confType = SmsConfTypeEnum.AUTO_INVEST_PART_PAY_SUCCESS;
                param.put("successAmount",successAmount.toString());
                param.put("failedAmount",failedAmount.toString());
            }
            SmsMessage smsMessage = SmsMessage.builder().mobile(mobile).confType(confType).param(param).build();
            amqpTemplate.convertAndSend(Exchange.SMS_NOTIFY_SEND.getRoutingKey(),smsMessage);
            redisTemplate.opsForValue().set(key,String.valueOf(orderNo));
            redisTemplate.expire(key,15, TimeUnit.DAYS);
            log.info("扣款完成短信通知完成，smsMessage={}",smsMessage);

        } catch (Exception e) {
            log.error("定投订单扣款完成发送短信异常",e);
        }

    }
}
