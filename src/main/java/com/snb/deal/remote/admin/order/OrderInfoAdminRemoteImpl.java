package com.snb.deal.remote.admin.order;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageInfo;
import com.snb.common.util.Model2DtoUtil;
import com.snb.deal.admin.api.dto.order.*;
import com.snb.deal.admin.api.remote.order.OrderInfoAdminRemote;
import com.snb.deal.bo.order.OrderInvestAdminBO;
import com.snb.deal.bo.order.OrderRebalanceAdminBO;
import com.snb.deal.bo.order.OrderRebalanceDetailBO;
import com.snb.deal.bo.order.OrderRedeemAdminBO;
import com.snb.deal.entity.order.OrderInvestDetailDO;
import com.snb.deal.entity.order.OrderRedeemDetailDO;
import com.snb.deal.service.order.OrderInvestService;
import com.snb.deal.service.order.OrderRebalanceService;
import com.snb.deal.service.order.OrderRedeemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单相关后台管理服务
 *
 * @author yunpeng.zhang
 */
@Slf4j
@Service(version = "1.0")
public class OrderInfoAdminRemoteImpl implements OrderInfoAdminRemote {
    @Resource
    private OrderInvestService orderInvestService;

    @Resource
    private OrderRedeemService orderRedeemService;

    @Resource
    private OrderRebalanceService orderRebalanceService;

    /**
     * 分页查询投资订单列表，返回数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public PageInfo<OrderInvestListDTO> pageOrderInvestList(OrderInvestListRequest request) {
        log.info("分页查询投资订单列表，request：{}", request);
        PageInfo<OrderInvestListDTO> result = new PageInfo<>();

        //1.根据用户手机号和商户交易流水号查询订单列表
        PageInfo<OrderInvestAdminBO> info;
        try {
            info = orderInvestService.pageOrderInvest(request);
        } catch (Exception e) {
            log.error("分页查询投资订单列表异常！request：{}", request, e);
            return result;
        }

        //2.组织数据
        if (info != null) {
            result = Model2DtoUtil.model2Dto(info, OrderInvestListDTO.class);
        } else {
            log.info("分页查询投资订单列表为空！request：{}", request);
        }
        return result;
    }

    /**
     * 分页查询调仓订单列表，返回数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public PageInfo<OrderRebalanceListDTO> pageOrderRebalanceList(OrderRebalanceListRequest request) {
        log.info("分页查询投资订单列表，request：{}", request);
        PageInfo<OrderRebalanceListDTO> result = new PageInfo<>();

        //1.根据用户手机号和商户交易流水号查询订单列表
        PageInfo<OrderRebalanceAdminBO> info;
        try {
            info = orderRebalanceService.pageOrderRebalance(request);
        } catch (Exception e) {
            log.error("分页查询投资订单列表异常！request：{}", request, e);
            return result;
        }

        //2.组织数据
        if (info != null) {
            result = Model2DtoUtil.model2Dto(info, OrderRebalanceListDTO.class);
        } else {
            log.info("分页查询投资订单列表为空！request：{}", request);
        }

        return result;
    }

    /**
     * 分页查询赎回订单列表，返回数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public PageInfo<OrderRedemptionListDTO> pageOrderRedemptionList(OrderRedemptionListRequest request) {
        log.info("分页查询赎回订单列表，request：{}", request);
        PageInfo<OrderRedemptionListDTO> result = new PageInfo<>();

        //1.根据用户手机号和商户交易流水号查询订单列表
        PageInfo<OrderRedeemAdminBO> info;
        try {
            info = orderRedeemService.pageOrderRedeem(request);
        } catch (Exception e) {
            log.error("分页查询赎回订单列表异常！request：{}", request, e);
            return result;
        }
        //2.组织数据
        if (info != null) {
            result = Model2DtoUtil.model2Dto(info, OrderRedemptionListDTO.class);
        } else {
            log.info("分页查询赎回订单列表为空！request：{}", request);
        }

        return result;
    }

    /**
     * 查询某个买入订单明细，返回数据可能为空
     *
     * @param condition
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public List<OrderInvestDetailDTO> getOrderInvestDetail(OrderInvestDetailRequest condition) {
        log.info("查询某个买入订单明细，request：{}", condition);
        List<OrderInvestDetailDTO> result = new ArrayList<>();
        //0. 参数校验
        if (condition == null || condition.getOrderInvestId() == null) {
            log.error("查询某个买入订单明细参数校验失败！");
            return result;
        }

        //1.根据买入订单id查询买入基金明细列表
        List<OrderInvestDetailDO> orderInvestDetailDOList = null;
        try {
            orderInvestDetailDOList = orderInvestService.listOrderInvestDetail(condition);
        } catch (Exception e) {
            log.error("查询某个买入订单明细异常！request：{}", condition, e);
            return result;
        }

        //2.组织数据
        if (CollectionUtils.isNotEmpty(orderInvestDetailDOList)) {
            result = Model2DtoUtil.model2Dto(orderInvestDetailDOList, OrderInvestDetailDTO.class);
        } else {
            log.info("查询某个买入订单明细为空！");
        }

        return result;
    }

    /**
     * 查询调仓订单明细，返回数据可能为空
     *
     * @param condition
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public List<OrderRebalanceDetailDTO> getOrderRebalanceDetail(OrderRebalanceDetailRequest condition) {
        log.info("查询调仓订单明细，request：{}", condition);
        List<OrderRebalanceDetailDTO> result = new ArrayList<>();
        //0.参数校验
        if (condition == null || condition.getOrderRebalanceId() == null) {
            log.error("查询调仓订单明细参数校验失败！");
            return result;
        }

        //1.根据赎回订单id查询买入基金明细列表
        List<OrderRebalanceDetailBO> orderRebalanceDetailList;
        try {
            orderRebalanceDetailList = orderRebalanceService.listOrderRebalanceDetail(condition);
        } catch (Exception e) {
            log.error("查询调仓订单明细异常！request：{}", condition, e);
            return result;
        }
        //2.组织数据
        if (CollectionUtils.isNotEmpty(orderRebalanceDetailList)) {
            result = Model2DtoUtil.model2Dto(orderRebalanceDetailList, OrderRebalanceDetailDTO.class);
        } else {
            log.info("查询调仓订单明细为空！");
        }
        return result;
    }

    /**
     * 查询某个赎回订单明细，返回数据可能为空
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    @Override
    public List<OrderRedemptionDetailDTO> getOrderRedemptionDetail(OrderRedemptionDetailRequest request) {
        log.info("查询某个赎回订单明细，request：{}", request);
        List<OrderRedemptionDetailDTO> result = new ArrayList<>();
        //0.参数校验
        if (request == null || request.getOrderRedeemId() == null) {
            log.error("查询某个赎回订单明细参数校验失败！");
            return result;
        }

        //1.根据条件查询买入基金明细列表
        List<OrderRedeemDetailDO> orderRedeemDOList;
        try {
            orderRedeemDOList = orderRedeemService.listOrderRedeemDetail(request);
        } catch (Exception e) {
            log.error("查询某个赎回订单明细异常！request：{}", request, e);
            return result;
        }

        //2.组织数据
        if (CollectionUtils.isNotEmpty(orderRedeemDOList)) {
            result = Model2DtoUtil.model2Dto(orderRedeemDOList, OrderRedemptionDetailDTO.class);
        }

        return result;
    }
}
