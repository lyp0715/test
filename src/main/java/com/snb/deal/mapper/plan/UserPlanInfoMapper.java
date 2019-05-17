package com.snb.deal.mapper.plan;


import com.snb.deal.admin.api.dto.plan.UserPlanInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlanInfoMapper {
    List<UserPlanInfo> queryUserPlanInfoDtoByCondition(@Param("phone") String phone, @Param("name") String name);
}