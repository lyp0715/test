package com.snb.deal.service.order;

import com.github.pagehelper.PageInfo;
import com.snb.common.enums.FundChannelEnum;
import com.snb.common.mq.bean.AutoInvestMessage;
import com.snb.deal.admin.api.dto.order.OrderInvestDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderInvestListRequest;
import com.snb.deal.api.dto.plan.PlanAutoInvestRequest;
import com.snb.deal.api.enums.order.InvestTypeEnum;
import com.snb.deal.bo.order.OrderInfoListBO;
import com.snb.deal.bo.order.OrderInvestAdminBO;
import com.snb.deal.bo.order.OrderInvestBO;
import com.snb.deal.bo.order.OrderListBO;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderInvestDO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.plan.PlanInfoDO;
import com.snb.third.api.BaseResponse;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.invest.YfFundInvestResponse;
import com.snb.third.yifeng.dto.order.invest.YfInvestResponse;

import java.util.Date;
import java.util.List;

public interface OrderInvestService {

    /**
     * 分页查询买入订单信息
     *
     * @param request
     * @return
     */
    PageInfo<OrderInvestAdminBO> pageOrderInvest(OrderInvestListRequest request);

    /**
     * 根据条件查询买入基金明细列表
     *
     * @param condition
     * @return
     */
    List<OrderInvestDetailDO> listOrderInvestDetail(OrderInvestDetailRequest condition);

    /**
     * 根据用户id查询用户计划定投记录详情
     *
     * @param request
     * @return
     */
    PageInfo<OrderInvestDO> pageOrderPlanAutoInvest(PlanAutoInvestRequest request);

    /**
     *  保存投资订单
     * @param orderInvestDO
     * @return
     * @throws Exception
     */
    OrderInvestDO createInvestOrder(OrderInvestDO orderInvestDO) throws Exception;

    /**
     * 投资发起失败
     * @return
     * @throws Exception
     */
    OrderInvestDO afterInvestFailed(Long orderInvestId,Long orderNo, String code, String message) throws Exception;

    /**
     * 组合购买发起成功
     * @param orderInvestDO
     * @param baseResponse
     * @return
     * @throws Exception
     */
    void afterInvestApply(OrderInvestDO orderInvestDO, BaseResponse<YfInvestResponse> baseResponse) throws Exception;

    /**
     * @param orderInfoDO
     * @param orderListBO
     * @return
     * @Description 订单列表获取手动购买
     * @author lizengqiang
     * @date 2018/4/18 20:27
     */
    List<OrderInfoListBO> getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO);

    /**
     * 查询同步订单列表
     * @param businessCode
     * @param orderStatus
     * @param channel
     * @return
     */
    List<OrderInvestBO> querySyncOrderList(String businessCode, Integer orderStatus, Integer channel);

    /**
     * 同步投资订单
     * @param orderInvestBO
     * @param syncOrderListResponse
     */
    void syncInvestOrder(OrderInvestBO orderInvestBO, SyncOrderListResponse syncOrderListResponse);

    /**
     * 自动定投
     * @param orderInvestDO 投资订单
     * @param syncOrderListResponse 订单详情
     * @return
     * @throws Exception
     */
    OrderInvestDO afterAutoInvest(PlanInfoDO planInfoDO, OrderInvestDO orderInvestDO, SyncOrderListResponse syncOrderListResponse) throws Exception;

    /**
     * 计算订单预计确认日期（订单明细中最大的预计确认日期（交易日期+基金申购确认日差））
     *
     * @author yunpeng.zhang
     */
    Date getOrderExpectedConfirmDate(Long orderInvestId);

    /**
     * 根据订单流水号和渠道查询投资订单
     * @param merchantNumber
     * @return
     */
    OrderInvestDO queryByMerchantNumber(String merchantNumber, FundChannelEnum channel);

    /**
     * 根据主键查询投资订单
     * @param orderInvestId
     * @return
     */
    OrderInvestDO queryByOrderInvestId(Long orderInvestId);

    /**
     * 补偿定投回调
     * @param autoInvestMessage
     * @throws Exception
     */
    void compensateAutoInvestCallback(AutoInvestMessage autoInvestMessage) throws Exception;

    /**
     * 基金购买发起成功
     * @param orderInvestDO
     * @param baseResponse
     */
    void afterFundInvestApply(OrderInvestDO orderInvestDO, BaseResponse<YfFundInvestResponse> baseResponse);

    /**
     * 用户投资次数
     * @param userId
     * @return
     */
    Long countUserInvestOrder(String userId, InvestTypeEnum investTypeEnum);
}
