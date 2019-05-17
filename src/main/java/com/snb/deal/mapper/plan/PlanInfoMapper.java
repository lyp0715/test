package com.snb.deal.mapper.plan;

import com.snb.deal.entity.plan.PlanInfoDO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.net.Inet4Address;
import java.util.Date;
import java.util.List;

@Repository
public interface PlanInfoMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanInfoDO record);

    /*@Select({
        "select",
        "id, plan_info_id, user_id, fund_user_account_id, third_plan_id, channel, name, ",
        "cycle, cycle_day, implement, postpone_deduction, portfolio_amount, target_amount, ",
        "portfolio_year, plan_status, create_time, update_time, yn",
        "from plan_info",
        "where id = #{id,jdbcType=BIGINT}"
    })
    @ResultMap("dao.PlanInfoMapper.BaseResultMap")
    PlanInfo selectByPrimaryKey(Long id);*/

    int updateThirdPlanId(@Param("planId") long planId,@Param("thirdPlanId") String thirdPlanId,@Param("planStatus") int planStatus);

    /**
     * 根据业务主键返回实体
     * @param planInfoId
     * @return
     */
    PlanInfoDO getByPlanInfoId(Long planInfoId);

    /**
     * 更新计划信息
     * @param planInfo
     * @return
     */
    int updatePlanInfo(PlanInfoDO planInfo);

    /**
     * 根据用户id和业务主键id查询计划信息
     * @param userId 用户id
     * @param planInfoId 计划id
     * @return
     */
    PlanInfoDO getUserPlanInfoById(@Param("userId") String userId, @Param("planInfoId") Long planInfoId);

    /**
     * 修改计划状态
     * @param planId 计划id
     * @param planStatus 计划状态
     * @return
     */
    int updatePlanStatus(@Param("planId") Long planId, @Param("planStatus") Integer planStatus);

    /**
     * 根据第三方计划id和渠道查询
     * @param thirdPlanId 第三方计划id
     * @param channel 渠道
     * @return
     */
    PlanInfoDO getByThirdPlanId(@Param("thirdPlanId") String thirdPlanId, @Param("channel") Integer channel);

    /**
     * 根据用户id查询计划列表
     * @param userId
     * @return
     * @author yunpeng.zhang
     */
    List<PlanInfoDO> listPlanInfo(String userId);

    /**
     * 更新计划下次执行日期
     * @param planInfoId
     * @param nextRunDate
     * @return
     */
    int updateNextRunDate(@Param("planInfoId") Long planInfoId, @Param("nextRunDate") Date nextRunDate);

    /**
     * 分页查询有效的计划列表
     * @param start
     * @param end
     * @return
     */
    List<PlanInfoDO> pageAvailablePlanByRunDate(@Param("nextRunDate") Date nextRunDate,@Param("start") Integer start,@Param("end") Integer end);

    List<PlanInfoDO> pageAvailablePlanBeforeRunDate(@Param("nextRunDate") Date nextRunDate,@Param("start") Integer start,@Param("end") Integer end);

    /**
     * 查询用户被系统暂停的计划
     * @param userId
     * @return
     */
    PlanInfoDO getSysSuspendPlanInfo(@Param("userId") String userId);

    List<PlanInfoDO> pagePlanInfo(@Param("start") Integer start, @Param("end") Integer end);
}