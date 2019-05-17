//package com.snb.deal.service.impl.flow;
//
//import com.snb.common.datetime.DateTimeUtil;
//import com.snb.deal.entity.flow.FlowNumberDO;
//import com.snb.deal.enums.FlowNumberTypeEnum;
//import com.snb.deal.mapper.flow.FlowNumberMapper;
//import com.snb.deal.service.flowno.FlowNumberService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang.RandomStringUtils;
//import org.apache.commons.lang.StringUtils;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//
//@Service
//@Slf4j
//public class FlowNumberDbServiceImpl implements FlowNumberService {
//
//
//    private static final char[] seed = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
//            'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
//    @Resource
//    private FlowNumberMapper flowNumberMapper;
//
//    @Override
//    public void generateFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) {
//        int length = 1000;
//        for (int i = 0; i < length; i++) {
//            this.generate(flowNumberTypeEnum);
//        }
//    }
//
//    private String generate(FlowNumberTypeEnum flowNumberTypeEnum) {
//        String flowNum;
//        do {
//            flowNum = this.generateNextFlowNum(flowNumberTypeEnum);
//        } while (StringUtils.isEmpty(flowNum));
//        return flowNum;
//    }
//
//    private String generateNextFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) {
//        String flowNum = this.getNextFlowNum();
//        try {
//            flowNumberMapper.insert(new FlowNumberDO(flowNum, flowNumberTypeEnum.getFlowType()));
//        } catch (Exception e) {
//            flowNum = null;
//            log.error("generateNextFlowNum is error", e);
//        }
//        return flowNum;
//    }
//
//    @Override
//    public String getFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) {
//        String flowNum;
//        do {
//            flowNum = this.getFlowNumFromDb(flowNumberTypeEnum);
//        } while (StringUtils.isEmpty(flowNum));
//        return flowNum;
//    }
//
//    private String getFlowNumFromDb(FlowNumberTypeEnum flowNumberTypeEnum) {
//        try {
//            FlowNumberDO flowNum = flowNumberMapper.get(new FlowNumberDO(flowNumberTypeEnum.getFlowType()));
//            if (flowNum == null) {
//                this.generateFlowNum(flowNumberTypeEnum);
//                flowNum = flowNumberMapper.get(new FlowNumberDO(flowNumberTypeEnum.getFlowType()));
//            }
//            int flag = flowNumberMapper.updateYnByVersion(new FlowNumberDO(flowNum.getFlowNum(), flowNum.getFlowType(), flowNum.getVersion()));
//            if (flag > 0) {
//                return flowNum.getFlowNum();
//            }
//        } catch (Exception e) {
//            log.error("getFlowNumFromDb is error", e);
//        }
//        return null;
//    }
//
//    private String getNextFlowNum() {
//        String random = RandomStringUtils.random(5, seed);
//        String dateStr = DateTimeUtil.getCurrentDatetime(DateTimeUtil.TimeFormat.SHORT_DATE_PATTERN_NONE);
//        return new StringBuffer("JLC").append(dateStr).append(random).toString();
//    }
//}
