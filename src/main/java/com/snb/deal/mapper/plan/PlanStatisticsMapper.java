package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanStatisticsDO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanStatisticsMapper {
    PlanStatisticsDO selectByPlanInfoId(@Param("planInfoId") Long planInfoId, @Param("userId") String userId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanStatisticsDO record);

    /*@Delete({
        "delete from plan_statistics",
        "where id = #{id,jdbcType=BIGINT}"
    })
    int deleteByPrimaryKey(Long id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanStatistics record);

    @Select({
        "select",
        "id, plan_statistics_id, user_id, plan_id, total_invest_amount, total_success_num, ",
        "total_failed_num, create_time, update_time, yn",
        "from plan_statistics",
        "where id = #{id,jdbcType=BIGINT}"
    })
    @ResultMap("dao.PlanStatisticsMapper.BaseResultMap")
    PlanStatistics selectByPrimaryKey(Long id);*/

}