package com.snb.deal.service.order;

import com.github.pagehelper.PageInfo;
import com.snb.deal.admin.api.dto.order.OrderRedemptionDetailRequest;
import com.snb.deal.admin.api.dto.order.OrderRedemptionListRequest;
import com.snb.deal.api.dto.redeem.OrderRedeemAmountResponse;
import com.snb.deal.api.dto.redeem.OrderRedeemFeeResponse;
import com.snb.deal.api.dto.redeem.OrderRedeemResponse;
import com.snb.deal.bo.order.*;
import com.snb.deal.entity.order.OrderInfoDO;
import com.snb.deal.entity.order.OrderRedeemDO;
import com.snb.deal.entity.order.OrderRedeemDetailDO;
import com.snb.deal.enums.ResultCode;
import com.snb.third.api.BaseResponse;
import com.snb.third.yifeng.dto.order.SyncOrderListResponse;
import com.snb.third.yifeng.dto.order.redeem.RedeemPortfolioResponse;

import java.util.List;

/**
 * @author lizengqiang
 * @Description 订单赎回服务
 * @date 2018/4/10 14:41
 */
public interface OrderRedeemService {

    /**
     * @param orderRedeemBO
     * @return
     * @Description 获取组合赎回金额相关信息
     * @author lizengqiang
     * @date 2018/4/12 20:51
     */
    OrderRedeemAmountResponse getOrderRedeemAmount(OrderRedeemBO orderRedeemBO);

    /**
     * @param orderRedeemBO
     * @return
     * @Description 创建赎回订单进行金额校验
     * @author lizengqiang
     * @date 2018/4/12 20:51
     */
    ResultCode checkOrderRedeem(OrderRedeemBO orderRedeemBO);

    /**
     * @param orderRedeemBO
     * @return
     * @Description 创建赎回订单
     * @author lizengqiang
     * @date 2018/4/12 20:51
     */
    OrderRedeemDO createOrderRedeem(OrderRedeemBO orderRedeemBO) throws Exception;

    /**
     * @return
     * @Description 根据交易状态查询赎回订单
     * @author lizengqiang
     * @date 2018/4/13 21:24
     */
    List<OrderRedeemDO> queryReceiving(int limit);

    /**
     * @param baseResponse
     * @param orderRedeemDO
     * @return
     * @Description 补偿赎回表，赎回详情表
     * @author lizengqiang
     * @date 2018/4/12 20:52
     */
    OrderRedeemResponse compensateOrderRedeem(BaseResponse<RedeemPortfolioResponse> baseResponse, OrderRedeemDO orderRedeemDO);
    /**
     * @param orderRedeemResponse
     * @param orderRedeemDO
     * @return
     * @Description 赎回完成页设置赎回金额、赎回费用、赎回基金交易预计确认日期、到帐日期
     * @author lizengqiang
     * @date 2018/6/6 11:16
     */
    void orderRedeemComplete(OrderRedeemResponse orderRedeemResponse, RedeemPortfolioResponse redeemPortfolioResponse, OrderRedeemDO orderRedeemDO) ;
    /**
     * @return
     * @Description 查询异步同步赎回订单请求集合
     * @author lizengqiang
     * @date 2018/4/13 11:07
     */
    List<OrderRedeemAsyncBO> queryOrderRedeemAsync(int pageNo, int pageSize);

    /**
     * @param orderRedeemAsyncBO
     * @param baseResponse
     * @return
     * @Description 查询订单同步赎回订单信息
     * @author lizengqiang
     * @date 2018/4/13 14:09
     */
    void syncOrderRedeem(OrderRedeemAsyncBO orderRedeemAsyncBO, BaseResponse<SyncOrderListResponse> baseResponse) throws Exception;

    /**
     * @param request
     * @return
     * @Description 根据条件查询订单列表(为后台管理准备的查询)
     * @author lizengqiang
     * @date 2018/4/13 21:47
     */
    PageInfo<OrderRedeemAdminBO> pageOrderRedeem(OrderRedemptionListRequest request);

    /**
     * 查询某个赎回订单明细
     *
     * @param request
     * @return
     */
    List<OrderRedeemDetailDO> listOrderRedeemDetail(OrderRedemptionDetailRequest request);

    /**
     * @param orderInfoDO
     * @param orderListBO
     * @return
     * @Description 订单列表获取赎回
     * @author lizengqiang
     * @date 2018/4/18 20:27
     */
    OrderInfoListBO getOrderList(OrderInfoDO orderInfoDO, OrderListBO orderListBO);

    /**
     * @param orderRedeemBO
     * @return
     * @Description 计算费率
     * @author lizengqiang
     * @date 2018/4/23 17:10
     */
    OrderRedeemFeeResponse orderRedeemFee(OrderRedeemBO orderRedeemBO) throws Exception;

    /**
     * @param merchantNumber
     * @return
     * @Description 根据交易流水号获取赎回订单
     * @author lizengqiang
     * @date 2018/5/2 10:59
     */
    List<OrderRedeemDO> queryByMerchantNumber(String merchantNumber,Integer channel) throws Exception;
    /**
     * @param mainModelId
     * @return
     * @Description 获取赎回的预计确认日期
     * @author lizengqiang
     * @date 2018/4/28 14:07
     */
    String getExpectedConfirmedDate(Long mainModelId) throws Exception;

    /**
     * @param forceOrderBO
     * @return
     * @Description 创建强制赎回订单
     * @author lizengqiang
     * @date 2018/5/24 17:07
     */
    void createForceOrderRedeem(ForceOrderBO forceOrderBO) throws Exception;

    /**
     * 同步赎回订单
     * @param orderRedeemAsyncBO
     * @throws Exception
     */
    void syncOrderRedeem(OrderRedeemAsyncBO orderRedeemAsyncBO) throws Exception;
}
