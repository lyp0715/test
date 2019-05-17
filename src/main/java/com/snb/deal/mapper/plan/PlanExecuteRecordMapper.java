package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanExecuteRecordDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanExecuteRecordMapper {

    PlanExecuteRecordDO selectByMerchantNumber(@Param("merchantNumber") String merchantNumber);

    int insertBatch(List<PlanExecuteRecordDO> planExecuteRecordDOList);

    PlanExecuteRecordDO selectByUserIdAndPlanInfoId(@Param("userId") String userId, @Param("planInfoId") Long planInfoInd);
}