package com.snb.deal.service.flowno;

import com.snb.deal.ApplicationMain;
import com.snb.deal.enums.FlowNumberTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class FlowNumberServiceTest {



    @Resource
    private FlowNumberService flowNumberService;

    @Test
    public void generateFlowNo() {
//        Long start=System.currentTimeMillis();
//        try {
//            flowNumberService.generateFlowNo(FlownoTypeEnum.YIFENG);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Long end=System.currentTimeMillis()-start;
//        System.out.println(end);
    }

    @Test
    public void getFlowNo() throws Exception {
        Long start=System.currentTimeMillis();
        String result=flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG);
        System.out.println(result);
        Long end=System.currentTimeMillis()-start;
        System.out.println(end);
    }


}
