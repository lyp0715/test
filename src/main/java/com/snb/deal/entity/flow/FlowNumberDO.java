package com.snb.deal.entity.flow;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.joda.time.DateTime;

import java.util.Date;

@Data
@NoArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class FlowNumberDO extends BaseBean {

    private String flowNum;

    private Integer flowType;

    private Integer version;

    private Date startDate;

    private Date endDate;


    public FlowNumberDO(Integer flowType){
        this.flowType=flowType;
        startDate=new DateTime().withMillisOfDay(0).toDate();
        endDate=new DateTime().plusDays(1).withMillisOfDay(0).minusMillis(1).toDate();
    }
    public FlowNumberDO(String flowNum, Integer flowType){
        this.flowNum=flowNum;
        this.flowType=flowType;
    }

    public FlowNumberDO(String flowNum, Integer flowType, Integer version){
        this.flowNum=flowNum;
        this.flowType=flowType;
        this.version=version;

    }

}