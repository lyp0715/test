package com.snb.deal.entity.plan;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 计划持仓模型
 */
@Data
@ToString
public class PlanPortfolioModel implements Serializable{

    private Long planPortfolioRelId;

    private String userId;

    private Long planId;

    private Long mainModelId;

    private String thirdPortfolioId;

    private Integer channel;

    private Date createTime;

    private Date updateTime;

    private String fundMainModelName;

    private String thirdPortfolioCode;

}
