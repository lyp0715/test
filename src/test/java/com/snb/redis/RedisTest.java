package com.snb.redis;

import com.snb.deal.ApplicationMain;
import com.snb.deal.mapper.order.OrderRedeemMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author lizengqiang
 * @Description
 * @date 2018/3/12 9:57
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
public class RedisTest {
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private OrderRedeemMapper orderRedeemMapper;

    @Test
    public void testSetAndGet() {
        try {
//            OrderRedeemDO orderRedeem = orderRedeemMapper.selectByPrimaryKey(1L);
//            redisTemplate.opsForValue().set("ping", orderRedeem);
//            OrderRedeemDO result = (OrderRedeemDO) redisTemplate.opsForValue().get("ping");
//            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
