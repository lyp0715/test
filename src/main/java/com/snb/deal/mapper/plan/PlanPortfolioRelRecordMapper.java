package com.snb.deal.mapper.plan;

import org.springframework.stereotype.Repository;

@Repository
public interface PlanPortfolioRelRecordMapper {

    /*@Insert({
        "insert into plan_portfolio_rel_record (id, paln_portfolio_rel_record_id, ",
        "user_id, plan_id, ",
        "third_portfolio_code, third_portfolio_id, ",
        "third_type, create_time, ",
        "update_time, yn)",
        "values (#{id,jdbcType=BIGINT}, #{palnPortfolioRelRecordId,jdbcType=BIGINT}, ",
        "#{userId,jdbcType=VARCHAR}, #{planId,jdbcType=VARCHAR}, ",
        "#{thirdPortfolioCode,jdbcType=VARCHAR}, #{thirdPortfolioId,jdbcType=VARCHAR}, ",
        "#{thirdType,jdbcType=BIT}, #{createTime,jdbcType=TIMESTAMP}, ",
        "#{updateTime,jdbcType=TIMESTAMP}, #{yn,jdbcType=BIT})"
    })
    int insert(PlanPortfolioRelRecord record);

    @Select({
        "select",
        "id, paln_portfolio_rel_record_id, user_id, plan_id, third_portfolio_code, third_portfolio_id, ",
        "third_type, create_time, update_time, yn",
        "from plan_portfolio_rel_record",
        "where id = #{id,jdbcType=BIGINT}"
    })
    @ResultMap("dao.PlanPortfolioRelRecordMapper.BaseResultMap")
    PlanPortfolioRelRecord selectByPrimaryKey(Long id);*/

}