package com.snb.deal.bo.order;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderDividendBO {

    private String userId;

    private String accountNumber;

    private Integer channel;
}
