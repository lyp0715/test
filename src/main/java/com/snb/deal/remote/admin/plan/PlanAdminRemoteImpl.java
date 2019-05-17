package com.snb.deal.remote.admin.plan;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.admin.api.dto.plan.AdminQueryCondition;
import com.snb.deal.admin.api.dto.plan.UserPlanInfo;
import com.snb.deal.admin.api.remote.plan.PlanAdminRemote;
import com.snb.deal.mapper.plan.UserPlanInfoMapper;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Service(version = "1.0")
public class PlanAdminRemoteImpl implements PlanAdminRemote {
    @Resource
    UserPlanInfoMapper userPlanInfoDtoMapper;

    @Override
    public APIResponse<PageInfo<UserPlanInfo>> queryUserPlanInfoByCondition(AdminQueryCondition request){
        Page<Map> page = PageHelper.startPage(request.getPage(), request.getPageSize());
        userPlanInfoDtoMapper.queryUserPlanInfoDtoByCondition(StringUtils.defaultIfBlank(request.getPhone(), null)
                , StringUtils.defaultIfBlank(request.getName(), null));
        return APIResponse.build(SystemResultCode.SUCCESS).setData(new PageInfo(page));
    }

}
