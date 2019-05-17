package com.snb.deal.biz.redeem;

import com.snb.deal.api.dto.redeem.OrderRedeemResponse;
import com.snb.deal.bo.order.OrderRedeemBO;

/**
 * @author lizengqiang
 * @Description 订单赎回
 * @date 2018/4/12 20:56
 */
public interface OrderRedeemBiz {

    /**
     * @param orderRedeemBO
     * @return
     * @Description 赎回
     * @author lizengqiang
     * @date 2018/4/28 20:05
     */
    OrderRedeemResponse orderRedeem(OrderRedeemBO orderRedeemBO) throws Exception;


    /**
     * @return
     * @Description 赎回同步
     * @author lizengqiang
     * @date 2018/4/28 20:06
     */
    void syncOrderRedeem() throws InterruptedException;

    /**
     * @return
     * @Description 赎回同步为回调使用
     * @author lizengqiang
     * @date 2018/4/28 20:06
     */
    void syncOrderRedeemCallBack(String merchantNumber,Integer channel) throws Exception;

    /**
     * @param userId
     * @param accountNumber
     * @return
     * @Description 同步强制赎回订单
     * @author lizengqiang
     * @date 2018/5/25 10:43
     */
    void syncForceOrderRedeem(String userId, String accountNumber);
}
