package com.snb.deal.bo.order;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@NoArgsConstructor
@ToString
public class OrderListBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 交易类型
     */
    private Integer businessCode;


    /**
     * 银行卡号
     */
    private String bankNumber;

    /**
     * 银行卡名称
     */
    private String bankName;
    /**
     * 银行卡logo
     */
    private String bankIcon;
    /**
     * 分页
     */
    private Integer pageSize = 20;
    /**
     * 分页
     */
    private Integer pageNo = 1;
}
