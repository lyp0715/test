package com.snb.deal.service.order;

import com.github.pagehelper.PageInfo;
import com.snb.deal.admin.api.dto.order.OrderRebalanceDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderRebalanceListRequest;
import com.snb.deal.api.dto.rebalance.RebalanceAuthResponse;
import com.snb.deal.api.dto.rebalance.RebalanceConfirmResponse;
import com.snb.deal.api.dto.rebalance.RebalanceRequest;
import com.snb.deal.api.dto.rebalance.RebalanceSummaryDTO;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.bo.order.OrderRebalanceAdminBO;
import com.snb.deal.bo.order.OrderRebalanceDetailBO;
import com.snb.deal.bo.rebalance.*;
import com.snb.deal.entity.order.*;
import com.snb.third.yifeng.dto.order.rebalance.YfRebalancePortfolioResponse;
import com.snb.third.yifeng.dto.order.rebalance.YfRebalanceSummaryResponse;

import java.util.List;

public interface OrderRebalanceService {


    PageInfo<OrderRebalanceAdminBO> pageOrderRebalance(OrderRebalanceListRequest request);

    List<OrderRebalanceDetailBO> listOrderRebalanceDetail(OrderRebalanceDetailRequest condition);

    /**
     * 创建调仓订单
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    void createOrderInfo(OrderRebalanceDO orderRebalanceDO);

    /**
     * 查询待同步的调仓订单
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    List<OrderRebalanceAsyncBO> queryOrderRebalanceAsync(Integer channel,Integer businessCode,Integer transactionStatus,Integer pagetIndex,Integer pageSize);

    /**
     * 调仓任务-同步订单处理
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    void syncOrderRebalance(OrderRebalanceAsyncBO orad, List<OrderRebalanceDetailDO> orderRebalanceDetailList) throws Exception;

    /**
     * 通过条件查询调仓订单数量
     * @author RunFa.Zhou
     * @date 2018-04-16
     * @return
     */
    Long queryOrderRebalanceCountBycondition(OrderRebalanceConditionBO orderRebalanceConditionBO);


    /**
     * 查询需要发送的调仓订单
     * @author RunFa.Zhou
     * @date 2018-04-19
     * @return
     */
    List<OrderRebalanceSendBO> querySendOrderRebalanceList(Integer code, int startIndex, int count);


    /**
     * 发起调仓任务-前处理
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    void rebalanceBeforeDeal(OrderRebalanceCreateBO rebalanceInfoBO,
                                         YfRebalanceSummaryResponse yfRebalanceSummaryResponse) throws Exception;

    /**
     * 发起调仓任务-后处理
     * @author RunFa.Zhou
     * @date 2018-04-14
     * @return
     */
    void rebalanceAfterDeal(YfRebalancePortfolioResponse rebalancePortfolioResponse, OrderRebalanceSendBO orderRebalanceSendBO);

    /**
     * @param orderInfoDO
     * @param orderListBO
     * @return
     * @Description 订单列表获取调仓
     * @author lizengqiang
     * @date 2018/4/18 20:27
     */
    OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO);

    /**
     * 调仓交易订单同步数据库处理
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    void syncOrderRebalance(List<OrderRebalanceDetailDO> addList, List<OrderRebalanceDetailDO> updateList,
                            OrderRebalanceDO orderRebalanceDO) throws Exception;

    /**
     *  查询调仓授权按钮显示
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    RebalanceAuthResponse getRebalanceAutnBtn(String userId, Integer channel) throws Exception;

    /**
     * 授权操作
     * @author RunFa.Zhou
     * @date 2018-04-26
     * @return
     */
    RebalanceAuthResponse doRebalanceAutn(String userId, Integer channel);

    /**
     * 调仓更新发送时间
     * @author RunFa.Zhou
     * @date 2018-04-27
     * @return
     */
    void updateOrderSendTime(OrderRebalanceSendBO orderRebalanceSendBO);

    /**
     * 作废之前的调仓数据
     * @author RunFa.Zhou
     * @date 2018-05-03
     * @return
     */
    void rebalanceAbandon(OrderRebalanceDO orderRebalanceDO) throws Exception;

    /**
     * 调仓发起失败
     * @author RunFa.Zhou
     * @date 2018-05-08
     * @return
     */
    void rebalanceStartFailure(OrderRebalanceSendBO orderRebalanceSendBO) throws Exception;

    /**
     * @author yunpeng.zhang
     */
    OrderRebalanceAuthDO getLastUnProcessRebalanceAuthByUserId(String userId);

    /**
     * @author yunpeng.zhang
     */
    OrderRebalanceDO getById(Long orderRebalanceId);

    /**
     * 获取最新的调仓摘要
     * @param orderRebalanceSummaryConditionBO
     * @return
     */
    OrderRebalanceSummaryDO getLastRebalanceSummaryByCondition(OrderRebalanceSummaryConditionBO orderRebalanceSummaryConditionBO);

    /**
     * 获取调仓摘要详情
     * @param orderRebalanceSummaryConditionBO
     * @return
     */
    List<OrderRebalanceSummaryDetailDO> getRebalanceSummaryDetailByCondition(OrderRebalanceSummaryConditionBO orderRebalanceSummaryConditionBO);

    /**
     * 确认调仓
     * @param requeset
     * @return
     */
    RebalanceConfirmResponse confirmRebalance(RebalanceRequest requeset);

    /**
     * 分页查询待修复的调仓历史数据集合
     * @param channel
     * @param businessCode
     * @param pagetIndex
     * @param pageSize
     * @return
     */
    List<OrderRebalanceAsyncBO> queryOrderRebalanceRepaired(Integer channel,Integer businessCode,Integer pagetIndex,Integer pageSize);
}
