package com.snb.deal.service.flowno;


import com.snb.deal.enums.FlowNumberTypeEnum;

/**
 * @Description
 * @author lizengqiang
 * @date 2018/4/14 13:27
 */
public interface FlowNumberService {

    String getFlowNum(FlowNumberTypeEnum flowNumberTypeEnum) throws Exception;
}
