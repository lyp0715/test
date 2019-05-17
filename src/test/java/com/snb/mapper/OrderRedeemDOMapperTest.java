package com.snb.mapper;

import com.jianlc.tc.guid.GuidCreater;
import com.snb.deal.ApplicationMain;
import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderRebalanceDetailRequest;
import com.snb.deal.bo.order.OrderRebalanceDetailBO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.plan.PlanChangeRecordDO;
import com.snb.deal.mapper.order.OrderInvestDetailMapper;
import com.snb.deal.mapper.order.OrderRebalanceDetailMapper;
import com.snb.deal.mapper.order.OrderRedeemMapper;
import com.snb.deal.mapper.plan.PlanChangeRecordMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/3/12 9:57
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class OrderRedeemDOMapperTest {
    @Resource
    private OrderRedeemMapper orderRedeemMapper;
    @Resource
    private OrderRebalanceDetailMapper orderRebalanceDetailMapper;

    @Resource
    private OrderInvestDetailMapper orderInvestDetailMapper;
    @Resource
    PlanChangeRecordMapper planChangeRecordMapper;
    @Resource
    GuidCreater guidCreater;

    @Test
    public void listByOrderRebalanceDetailCondition() {
        OrderRebalanceDetailRequest orderRebalanceDetailRequest = new OrderRebalanceDetailRequest();
        orderRebalanceDetailRequest.setOrderRebalanceId(47689745494016L);
        List<OrderRebalanceDetailBO> orderRebalanceDetailList = orderRebalanceDetailMapper.listByOrderRebalanceDetailCondition(orderRebalanceDetailRequest);
        System.out.println(orderRebalanceDetailList.size());
    }

    @Test
    public void listByOrderInvestDetailCondition() {
        OrderInvestDetailRequest orderInvestDetailRequest = new OrderInvestDetailRequest();
        orderInvestDetailRequest.setOrderInvestId(47403864268864L);
        List<OrderInvestDetailDO> orderInvestDetailDOList = orderInvestDetailMapper.listByOrderInvestDetailCondition(orderInvestDetailRequest);
        System.out.println(orderInvestDetailDOList.size());
    }

    @Test
    public void insertChange() {
        //保存修改计划前定投信息
        PlanChangeRecordDO planChangeRecord = new PlanChangeRecordDO();
        planChangeRecord.setChannel(1);
        planChangeRecord.setCycle(1);
        planChangeRecord.setCycleDay(15);
        planChangeRecord.setPlanInfoId(123L);
        planChangeRecord.setPlanName("test");
        planChangeRecord.setPortfolioAmount(new BigDecimal(500000));
        planChangeRecord.setUserId("2223321");
        planChangeRecord.setThirdPlanId("String");
        planChangeRecord.setPlanchangeRecordId(guidCreater.getUniqueID());
        planChangeRecordMapper.insertChange(planChangeRecord);
        System.out.println("ok");
    }
}
