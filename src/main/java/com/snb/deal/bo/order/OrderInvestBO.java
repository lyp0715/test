package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class OrderInvestBO implements Serializable{

    private String userId;

    private Long orderNo;

    private Integer orderStatus;

    private Integer channel;

    private String accountNumber;

    private String merchantNumber;

    private Long orderInvestId;

    private String portfolioCode;

    private Integer investType;

}
