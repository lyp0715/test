package com.snb;

import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.deal.api.remote.callback.IfastCallbackRemote;
import com.snb.user.util.JSONUtil;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * USER:    huangyunxing
 * TIME:    2018-07-11 14:37
 * COMMENT:
 */
public class TestIfastCallbackRemote extends BaseBeanTest {
    @Resource
    private IfastCallbackRemote remote;

    @Test
    public void testAutoInvest() {
        String json = "{\"merchantNumber\":\"JLC20180711000040619\",\"rspId\":\"26269\"}";
        AutoInvestMessage message = JSONUtil.parse(json, AutoInvestMessage.class);
        message.setChannel(FundChannelEnum.YIFENG);
        message.setNextRunDate("");
        message.setThirdPlanId(System.currentTimeMillis() + "");
        remote.autoInvestCallback(message);
    }


}
