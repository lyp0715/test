package com.snb;

import com.alibaba.fastjson.JSON;
import com.snb.deal.ApplicationMain;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class BaseBeanTest {

    protected String toJSON(Object o) {
        return JSON.toJSONString(o);
    }
}
