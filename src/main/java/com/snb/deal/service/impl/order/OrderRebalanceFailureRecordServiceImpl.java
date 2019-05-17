package com.snb.deal.service.impl.order;

import com.alibaba.fastjson.JSONObject;
import com.snb.deal.entity.order.OrderRebalanceFailureRecordDO;
import com.snb.deal.mapper.order.OrderRebalanceFailureRecordMapper;
import com.snb.deal.service.order.OrderRebalanceFailureRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service
@Slf4j
public class OrderRebalanceFailureRecordServiceImpl implements OrderRebalanceFailureRecordService {

    @Resource
    OrderRebalanceFailureRecordMapper orderRebalanceFailureRecordMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void insert(OrderRebalanceFailureRecordDO orderRebalanceFailureRecordDO) {
        int i = orderRebalanceFailureRecordMapper.insert(orderRebalanceFailureRecordDO);
        if (i <= 0) {
            log.error("新增调仓失败信息错误={}", JSONObject.toJSONString(orderRebalanceFailureRecordDO));
            throw new RuntimeException("新增调仓失败信息错误=" + orderRebalanceFailureRecordDO.getMerchantNumber());
        }
    }
}
