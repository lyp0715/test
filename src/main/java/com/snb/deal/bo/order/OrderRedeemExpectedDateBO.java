package com.snb.deal.bo.order;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@ToString
public class OrderRedeemExpectedDateBO implements Serializable {

    private static final long serialVersionUID = -3060056374940905323L;

    /**
     * 赎回基金交易预计确认日期
     */
    private Date expectedConfirmedDate;

    /**
     * 到帐日期
     */
    private Date expectedDealDate;
}
