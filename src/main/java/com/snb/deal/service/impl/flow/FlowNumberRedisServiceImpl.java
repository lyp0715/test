package com.snb.deal.service.impl.flow;

import com.snb.common.datetime.DateTimeUtil;
import com.snb.deal.entity.flow.FlowNumberDO;
import com.snb.deal.enums.FlowNumberTypeEnum;
import com.snb.deal.mapper.flow.FlowNumberMapper;
import com.snb.deal.service.flowno.FlowNumberService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("flowNumberService")
@Slf4j
public class FlowNumberRedisServiceImpl implements FlowNumberService {


    private static final char[] seed = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
    @Resource
    private FlowNumberMapper flowNumberMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Environment environment;

    private static final String FLOW_REDIS_LIST = "flowRedisList";

    @Override
    public String getFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) throws Exception {
        String flowNum;
        do {
            flowNum = this.getFlowNumFromRedis(flowNumberTypeEnum);
        } while (StringUtils.isEmpty(flowNum));
        log.info("FlowNumberService-getFlowNum,flowNum:{}", flowNum);
        return flowNum;
    }

    private String getFlowNumFromRedis(FlowNumberTypeEnum flowNumberTypeEnum) throws Exception {
        try {
            String flowNum = this.getNextFlowNum();
            String dateStr = DateTimeUtil.getCurrentDatetime(DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_NONE);
            String key = new StringBuffer(flowNumberTypeEnum.getKey()).append(dateStr).toString();
            Long flag = stringRedisTemplate.opsForSet().add(key, flowNum);
            if (flag > 0) {
                stringRedisTemplate.opsForList().leftPush(FLOW_REDIS_LIST, flowNum);
                return flowNum;
            }
            return null;
        } catch (Exception e) {
            log.error("getFlowNumFromRedis is error", e);
        }
        return this.getFlowNumFromDb(flowNumberTypeEnum);
    }

    private String getFlowNumFromDb(FlowNumberTypeEnum flowNumberTypeEnum) throws Exception {
        String flowNum;
        for (int i = 0; i <= 100; i++) {
            flowNum = this.generateNextFlowNum(flowNumberTypeEnum);
            if (StringUtils.isNotEmpty(flowNum)) {
                return flowNum;
            }
        }
        throw new Exception("FlowNumberService-getFlowNumFromDb is error");
    }

    private String generateNextFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) {
        String flowNum = this.getNextFlowNum();
        try {
            flowNumberMapper.insert(new FlowNumberDO(flowNum, flowNumberTypeEnum.getFlowType()));
        } catch (Exception e) {
            log.error("generateNextFlowNum is error", e);
            flowNum = null;
        }
        return flowNum;
    }

    private String getNextFlowNum() {
        String random = RandomStringUtils.random(5, seed);
        String dateStr = DateTimeUtil.getCurrentDatetime(DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_NONE);
        return new StringBuffer(environment.getProperty("ifast.api.key","WLJ")).append(dateStr).append(random).toString();
    }


    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public void syncFlowNumber() {
        for (int i = 0; i < 100; i++) {
            String flowNum = null;
            try {
                flowNum = stringRedisTemplate.opsForList().rightPop(FLOW_REDIS_LIST);
            } catch (Exception e) {
                log.error("flowNumberMapper-redis is error", e);
            }
            if (StringUtils.isEmpty(flowNum)) {
                continue;
            }
            try {
                FlowNumberDO flowNumberDO = new FlowNumberDO(flowNum, FlowNumberTypeEnum.YIFENG.getFlowType());
                flowNumberMapper.insert(flowNumberDO);
                log.info("{}-syncFlowNumber insert FlowNumberDO:{}", Thread.currentThread(), flowNumberDO.toString());
            } catch (Exception e) {
                log.error("flowNumberMapper-insert is error", e);
            }
        }
    }
}
