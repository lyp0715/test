package com.snb.deal.mapper.flow;


import com.snb.deal.entity.flow.FlowNumberDO;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowNumberMapper {

    int insert(FlowNumberDO flowNumber);

    int updateYnByVersion(FlowNumberDO flowNumber);

    FlowNumberDO get(FlowNumberDO flowNumber);

}