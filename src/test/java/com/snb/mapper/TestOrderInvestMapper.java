package com.snb.mapper;

import com.snb.BaseBeanTest;
import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.mapper.order.OrderInvestMapper;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * USER:    huangyunxing
 * TIME:    2018-07-11 15:46
 * COMMENT:
 */
public class TestOrderInvestMapper extends BaseBeanTest {
    @Resource
    private OrderInvestMapper orderInvestMapper;

    @Test
    public void test() {
        orderInvestMapper.querySyncOrderList("1,2".split(","), 1, FundChannelEnum.YIFENG.getChannel());
    }
}
