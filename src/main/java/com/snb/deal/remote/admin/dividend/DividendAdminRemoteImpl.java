package com.snb.deal.remote.admin.dividend;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.snb.common.dto.APIResponse;
import com.snb.common.dto.SystemResultCode;
import com.snb.deal.admin.api.dto.dividend.UserDividendDTO;
import com.snb.deal.admin.api.dto.dividend.UserDividendRequest;
import com.snb.deal.admin.api.remote.dividend.DividendAdminRemote;
import com.snb.deal.mapper.dividend.UserDividendDtoMapper;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Service(version = "1.0")
public class DividendAdminRemoteImpl implements DividendAdminRemote {
    @Resource
    UserDividendDtoMapper userDividedDtoMapper;

    @Override
    public APIResponse<PageInfo<UserDividendDTO>> queryUserDividedInfoByCondition(UserDividendRequest request){
        Page<Map> page = PageHelper.startPage(request.getPage(), request.getPageSize());
        userDividedDtoMapper.queryUserDividedInfoDtoByCondition(StringUtils.defaultIfBlank(request.getPhone(), null)
                , StringUtils.defaultIfBlank(request.getName(), null));
        return APIResponse.build(SystemResultCode.SUCCESS).setData(new PageInfo(page));
    }
}
