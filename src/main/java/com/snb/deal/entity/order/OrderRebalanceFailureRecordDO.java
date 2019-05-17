package com.snb.deal.entity.order;

import com.snb.common.bean.BaseBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 调仓失败记录DO
 * @author RunFa.Zhou
 * @date 2018-04-24
 * @return
 */
@Data
@NoArgsConstructor
@ToString(callSuper=true, includeFieldNames=true)
public class OrderRebalanceFailureRecordDO extends BaseBean {


    /**
     * 业务主键id
     */
    private Long orderRebalanceFailureId;

    /**
     * 用户Id
     */
    private String userId;

    /**
     * 流水号
     */
    private String merchantNumber;

    /**
     * 调仓业务类型:1调仓概要信息2调仓交易发送3调仓交易同步
     */
    private Integer rebalanceType;

    /**
     * 请求信息
     */
    private String requestInfo;

    /**
     * 响应信息
     */
    private String responseInfo;

    /**
     * 错误信息
     */
    private String errMessage;

    /**
     * 响应代码
     */
    private String errCode;

    /**
     * 渠道
     */
    private Integer channel;


}
