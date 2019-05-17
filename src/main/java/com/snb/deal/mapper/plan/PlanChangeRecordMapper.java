package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanChangeRecordDO;

import org.springframework.stereotype.Repository;

@Repository
public interface PlanChangeRecordMapper {

    int insertChange(PlanChangeRecordDO planChangeRecord);
}