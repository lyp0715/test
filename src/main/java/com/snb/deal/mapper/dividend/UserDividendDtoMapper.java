package com.snb.deal.mapper.dividend;

import com.snb.deal.admin.api.dto.dividend.UserDividendDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface UserDividendDtoMapper {
    List<UserDividendDTO> queryUserDividedInfoDtoByCondition(@Param("phone") String phone, @Param("name") String name);
}
