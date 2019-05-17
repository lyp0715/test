package com.snb.biz.rebalance;


import com.snb.deal.ApplicationMain;
import com.snb.deal.api.dto.rebalance.RebalanceAntuRequest;
import com.snb.deal.api.remote.rebalance.RebalanceRemote;
import com.snb.deal.bo.rebalance.OrderRebalanceSummaryConditionBO;
import com.snb.deal.entity.order.OrderRebalanceDO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDO;
import com.snb.deal.entity.order.OrderRebalanceSummaryDetailDO;
import com.snb.deal.enums.OrderRebalanceTransactionTypeEnum;
import com.snb.deal.mapper.order.OrderRebalanceSummaryDetailMapper;
import com.snb.deal.mapper.order.OrderRebalanceSummaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
@Slf4j
public class RebalanceRemoteTest {

    @Resource
    RebalanceRemote rebalanceRemote;

    //@Test
    public void rebalanceAutnBtn(){
        RebalanceAntuRequest request = new RebalanceAntuRequest();
        request.setUserId("JLC20180330000026962TEST");
        request.setChannel(1);
        rebalanceRemote.getRebalanceAutnBtn(request);
    }

    // 调仓交易授权
    //@Test
    public void doRebalanceAutn(){
        RebalanceAntuRequest request = new RebalanceAntuRequest();
        request.setUserId("JLC20180330000026962TEST");
        request.setChannel(1);
        rebalanceRemote.doRebalanceAutn(request);
    }

    @Resource
    Environment environment;

    @Resource
    OrderRebalanceSummaryMapper orderRebalanceSummaryMapper;

    @Resource
    OrderRebalanceSummaryDetailMapper orderRebalanceSummaryDetailMapper;

    @Test
    public void testStart(){

        OrderRebalanceDO orderRebalanceDO = new OrderRebalanceDO();
        orderRebalanceDO.setOrderRebalanceId(40993929670784L);

        OrderRebalanceSummaryConditionBO querySummaryObj = new OrderRebalanceSummaryConditionBO();
        querySummaryObj.setOrderRebalaceId(orderRebalanceDO.getOrderRebalanceId());

        OrderRebalanceSummaryDO orderRebalanceSummaryDO = orderRebalanceSummaryMapper.queryByCondition(querySummaryObj);

        //调仓比例
        OrderRebalanceSummaryConditionBO querySummaryDeatilObj = new OrderRebalanceSummaryConditionBO();
        querySummaryDeatilObj.setOrderRebalanceSummaryId(orderRebalanceSummaryDO.getOrderRebalanceSummaryId());
        querySummaryDeatilObj.setTransactionType(OrderRebalanceTransactionTypeEnum.ORDERREBALANCE_INVEST.getCode());


        List<OrderRebalanceSummaryDetailDO> orderRebalanceSummaryDetailDOList =
                orderRebalanceSummaryDetailMapper.queryOrderRebalanceSummaryDetail(querySummaryDeatilObj);

        BigDecimal rebalanceRate = BigDecimal.ZERO;
        for(OrderRebalanceSummaryDetailDO orderRebalanceSummaryDetailDO : orderRebalanceSummaryDetailDOList){
            rebalanceRate = rebalanceRate.add((orderRebalanceSummaryDetailDO.getPostProportion()
                    .subtract(orderRebalanceSummaryDetailDO.getPreProportion())).abs());
        }
//        BigDecimal r = rebalanceRate;
    }
}
