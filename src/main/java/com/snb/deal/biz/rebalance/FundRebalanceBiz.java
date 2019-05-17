package com.snb.deal.biz.rebalance;

import com.snb.deal.bo.rebalance.OrderRebalanceAsyncBO;
import com.snb.deal.bo.rebalance.OrderRebalanceCreateBO;
import com.snb.deal.bo.rebalance.OrderRebalanceSendBO;
import com.snb.third.api.BaseResponse;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.rebalance.YfRebalancePortfolioResponse;

public interface FundRebalanceBiz {

    /**
     * 调仓定时任务
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-11
     */
    void createRebalance() throws Exception;

    /**
     * 发送调仓交易
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-19
     */
    void sendRebalance() throws Exception;

    /**
     * 用户发送调仓交易
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-19
     */
    void singleSendRebalance(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception;


    /**
     * 单用户创建调仓交易
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-19
     */
    void singleCreateRebalance(OrderRebalanceCreateBO rebalanceInfoBO) throws Exception;


    /**
     * 同步调仓订单明细
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-14
     */
    void syncOrderRebalanceBiz(OrderRebalanceAsyncBO orad, BaseResponse<SyncOrderListResponse> baseResponse) throws Exception;


    /**
     * 调仓订单同步
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-27
     */
    boolean rebalanceOrderSync() throws Exception;

    /**
     * 调仓订单同步
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-28
     */
    void orderRebalanceAsync(OrderRebalanceAsyncBO orderRebalanceAsyncBO) throws Exception;

    /**
     * 确认调仓
     * @param orderRebalanceSendBO
     * @return
     */
    BaseResponse<YfRebalancePortfolioResponse> confirmRebalance(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception;

    /**
     * 修复调仓订单历史数据
     *
     * @return
     * @author RunFa.Zhou
     * @date 2018-04-27
     */
    boolean repairRebalanceOrder() throws Exception;
}
