package com.snb.deal.entity.incomesync;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class FundIncomeSyncDO {
    private Long id;

    private Long userFundIncomeSyncId;

    private String userId;

    private Long planPortfolioRelId;

    private Date syncDate;

    private Integer syncStatus;

    private Date syncStartDate;

    private Date syncEndDate;

    private Date createTime;

    private Date updateTime;

    private Integer yn;

}