package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanPortfolioModel;
import com.snb.deal.entity.plan.PlanPortfolioRelDO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanPortfolioRelMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanPortfolioRelDO record);

    List<PlanPortfolioRelDO> listPlanPortfolioRel(@Param("userId") String userId, @Param("planId") Long planId, @Param("channel") Integer channel);

    /**
     * 通过模型Id查询用户-调仓定时任务
     * @author RunFa.Zhou
     * @date 2018-04-13
     * @return
     */
    List<PlanPortfolioRelDO> getBatchRebalanceUserList(@Param("fundMainModelId") Long fundMainModelId,@Param("startIndex") int startIndex,@Param("count") int count);

    /*@Select({
        "select",
        "id, plan_portfolio_rel_id, user_id, plan_id, main_model_id, third_portfolio_id, ",
        "channel, create_time, update_time, yn",
        "from plan_portfolio_rel",
        "where id = #{id,jdbcType=BIGINT}"
    })
    @ResultMap("dao.PlanPortfolioRelMapper.BaseResultMap")
    PlanPortfolioRel selectByPrimaryKey(Long id);
*/

    /**
     * 查询用户计划持仓组合信息
     * @param userId 用户id
     * @param planId 计划id
     * @return
     */
    List<PlanPortfolioModel> selectUserPlanPortfolioModel(@Param("userId") String userId, @Param("planId") Long planId);


    List<PlanPortfolioModel> selectPlanPortfolioModelByUserId(@Param("userId") String userId);

    List<PlanPortfolioRelDO> selectByChannel(@Param("channel") Integer channel);

    /**
     * 更新第三方持仓id
     * @param planPortfolioRelId
     * @param thirdPortfolioId
     * @return
     */
    int updateThirdPortfolioId(@Param("planPortfolioRelId") Long planPortfolioRelId, @Param("thirdPortfolioId") String thirdPortfolioId);

    List<PlanPortfolioRelDO> selectByUserIdAndChannel(@Param("userId") String userId, @Param("channel") Integer channel);

    PlanPortfolioRelDO selectByPortfolioId(@Param("userId") String userId, @Param("portfolioId") String portfolioId, @Param("channel") Integer channel);
}