package com.snb.deal.service.impl.order;

import com.snb.BaseBeanTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class OrderRebalanceServiceImplTest extends BaseBeanTest{

    @Autowired
    private OrderRebalanceServiceImpl orderRebalanceService;

    @Test
    public void getRebalanceAutnBtn() throws Exception {
        orderRebalanceService.getRebalanceAutnBtn("ba8b203998014664a6641a908f73e8f7", 1);
    }
}