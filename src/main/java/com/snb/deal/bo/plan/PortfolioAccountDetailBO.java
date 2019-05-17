package com.snb.deal.bo.plan;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
public class PortfolioAccountDetailBO {
    /**
     * 累计收益
     */
    private BigDecimal totalIncome = BigDecimal.ZERO;

    /**
     * 基金可处理份额。单位为份，精准到0.01，如100000.00份
     */
    private BigDecimal availableUnit = BigDecimal.ZERO;

    /**
     * 是否可以购买
     */
    private String buyEnable;

    /**
     * 收费模式。0=前端收费，1=后端收费
     */
    private String chargeMode;

    /**
     * 资产现值。一般等于：总份额×最新净值+在途资产+未分配收益
     */
    private BigDecimal currentValue = BigDecimal.ZERO;

    /**
     * 持有的基金目前为止收到的所有现金分红。单位为元，精准到0.01，如10.01元
     */
    private BigDecimal dividendCash = BigDecimal.ZERO;

    /**
     * 分红方式。0=红利再投，1=现金分红
     */
    private String dividendInstruction;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金简称
     */
    private String fundName;

    /**
     * 基金类型
     */
    private String fundType;

    /**
     * 在途资产
     */
    private BigDecimal intransitAmount = BigDecimal.ZERO;

    /**
     * 资产成本。所有份额的总投资成本
     */
    private BigDecimal investmentAmount = BigDecimal.ZERO;


    /**
     * 最新净值。精准到4位小数，如1.0000
     */
    private BigDecimal nav;

    /**
     * 净值日期
     */
    private Date navDate;

    /**
     * 当前持仓盈亏。基金现值-基金成本
     */
    private BigDecimal profitLoss = BigDecimal.ZERO;

    /**
     * 昨日收益。根据最上2个净值对比得到的（货币型使用万份收益计算得到）,如果返回null，表示还在计算中
     */
    private BigDecimal yesterdayIncome = BigDecimal.ZERO;

    /**
     * 是否可以赎回
     */
    private String sellEnable;

    /**
     * 基金持有总份额。单位为份，精准到0.01，如100000.00份
     */
    private BigDecimal totalUnit = BigDecimal.ZERO;

    /**
     * 未分配收益。对于不是每日结转的货币型基金，未分配的收益（即未结转为份额的收益）
     */
    private BigDecimal undistributedIncome = BigDecimal.ZERO;

    /**
     * 占比
     */
    private BigDecimal proportion;

}
