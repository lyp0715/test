package com.snb.deal.mapper.insurance;

import com.snb.deal.admin.api.dto.insurance.InsuranceDTO;
import com.snb.deal.admin.api.dto.insurance.InsuranceParam;
import com.snb.deal.admin.api.dto.insurance.InsuranceRequest;
import com.snb.deal.admin.api.dto.insurance.InsuranceResultDto;
import com.snb.deal.entity.insurance.InsuranceDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceMapper {
    /**
     * 保存保险相关信息
     *
     * @param record 保险相关信息
     * @return
     * @author zhangyafeng
     */
    int insert(InsuranceDO record);

    /**
     * 更新用户保单状态
     *
     * @param userId 用户
     * @param status
     * @return
     * @author zhangyafeng
     */
    int updateInsuranceStatus(@Param("userId") String userId, @Param("status") int status);

    /**
     * 根据用户id和计划id查询保险信息
     *
     * @param userId      用户
     * @param insuranceId 保险id
     * @return
     * @author yunpeng.zhang
     */
    InsuranceDO selectByIdOrUserId(@Param("insuranceId") Long insuranceId, @Param("userId") String userId);

    /**
     * 查询用户保险信息
     *
     * @param userId
     * @return
     */
    List<InsuranceDO> selectByUserId(@Param("userId") String userId);

    /**
     * 批量更新保险信息
     *
     * @param userId
     * @return
     */
    int update(@Param("list") List<InsuranceDTO> userId);

    /**
     * 更新用户保险信息
     *
     * @param userId
     * @return
     */
    int deleteInsurance(@Param("userId") String userId);

    /**
     * 根据时间段获取保险信息
     * @param param
     * @return
     */
    List<InsuranceResultDto> selectInsuranceByTime(InsuranceParam param);
}