package com.snb.deal.mapper.plan;

import com.snb.deal.ApplicationMain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class PlanPortfolioRelMapperTest {

    @Resource
    PlanPortfolioRelMapper planPortfolioRelMapper;

    @Test
    public void selectByUserIdAndPlanInfoId() {

    }
}