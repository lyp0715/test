package com.snb.deal.mapper.order;


import com.snb.deal.admin.api.dto.order.OrderRedemptionDetailRequest;
import com.snb.deal.entity.order.OrderRedeemDetailDO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRedeemDetailMapper {
    /**
     * @param orderRedeemDetailDO
     * @return
     * @Description 插入
     * @author lizengqiang
     * @date 2018/4/28 18:58
     */
    int insert(OrderRedeemDetailDO orderRedeemDetailDO);

    /**
     * @param list
     * @return
     * @Description 批量插入
     * @author lizengqiang
     * @date 2018/4/28 18:58
     */
    int insertBatch(List<OrderRedeemDetailDO> list);

    /**
     * @param orderRedeemId
     * @return
     * @Description 根据赎回订单主键获取赎回详情信息
     * @author lizengqiang
     * @date 2018/4/28 18:58
     */
    List<OrderRedeemDetailDO> queryByRedeemId(Long orderRedeemId);

    /**
     * @param orderRedeemDetailId
     * @return
     * @Description 删除
     * @author lizengqiang
     * @date 2018/4/28 18:59
     */
    int delete(Long orderRedeemDetailId);

    /**
     * @param orderRedeemDetailDO
     * @return
     * @Description 更新
     * @author lizengqiang
     * @date 2018/4/28 18:59
     */
    int update(OrderRedeemDetailDO orderRedeemDetailDO);

    /**
     * 根据赎回订单id查询赎回明细
     *
     * @param request
     * @return
     * @author yunpeng.zhang
     */
    List<OrderRedeemDetailDO> listOrderRedeemDetail(OrderRedemptionDetailRequest request);
}