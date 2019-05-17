package com.snb.biz.rebalance;

import com.snb.common.enums.FundChannelEnum;
import com.snb.deal.ApplicationMain;
import com.snb.deal.biz.rebalance.FundRebalanceBiz;
import com.snb.deal.bo.rebalance.OrderRebalanceSendBO;
import com.snb.deal.enums.FlowNumberTypeEnum;
import com.snb.deal.service.flowno.FlowNumberService;
import com.snb.deal.service.order.OrderRebalanceService;
import com.snb.third.api.deal.FundPortfolioService;
import com.snb.third.yifeng.dto.order.rebalance.YfRebalanceDetail;
import com.snb.third.yifeng.dto.order.rebalance.YfRebalancePortfolioResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 调仓定时任务测试
 * @author RunFa.Zhou
 * @date 2018-04-23
 * @return
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationMain.class)
@Slf4j
public class FundRebalanceBizTest {

    @Resource
    FundRebalanceBiz fundRebalanceBiz;

    @Resource
    FlowNumberService flowNumberService;

    @Resource
    OrderRebalanceService orderRebalanceService;

    @Resource
    FundPortfolioService fundPortfolioService;


    //@Test
    public void singleSendRebalance() throws Exception {
        OrderRebalanceSendBO orderRebalanceSendBO = new OrderRebalanceSendBO();
        orderRebalanceSendBO.setThirdAccountNumber("JLC20180330000026962");
        orderRebalanceSendBO.setThirdPortfolioCode("TESTPP");
        orderRebalanceSendBO.setChannel(FundChannelEnum.YIFENG.getChannel());
        orderRebalanceSendBO.setThirdPortfolioId("986303");
        orderRebalanceSendBO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
        orderRebalanceSendBO.setOrderRebalanceId(888888L);

        try {
            fundRebalanceBiz.singleSendRebalance(orderRebalanceSendBO);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(),e);
        }
    }

    @Resource
    OrderRebalanceService rebalanceService;


    /**
     * 测试保存-调仓订单明细
     * @author RunFa.Zhou
     * @date 2018-04-23
     * @return
     */
    //@Test
    public void saveSendRebalance() throws Exception {
        OrderRebalanceSendBO orderRebalanceSendBO = new OrderRebalanceSendBO();
        orderRebalanceSendBO.setThirdAccountNumber("JLC20180330000026962");
        orderRebalanceSendBO.setThirdPortfolioCode("TESTPP");
        orderRebalanceSendBO.setChannel(FundChannelEnum.YIFENG.getChannel());
        orderRebalanceSendBO.setThirdPortfolioId("986303");

        String flowNum = flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG);

        orderRebalanceSendBO.setMerchantNumber(flowNumberService.getFlowNum(FlowNumberTypeEnum.YIFENG));
        orderRebalanceSendBO.setOrderRebalanceId(888888L);

        YfRebalancePortfolioResponse yfRebalancePortfolioResponse = new YfRebalancePortfolioResponse();

        yfRebalancePortfolioResponse.setMerchantNumber(flowNum);

        List<YfRebalanceDetail> rebalanceResult = new ArrayList<YfRebalanceDetail>();

        YfRebalanceDetail yfRebalanceDetailSell = new YfRebalanceDetail();

        yfRebalanceDetailSell.setContractNumber("ORD20180423000059480");
        yfRebalanceDetailSell.setFundCode("002214");
        yfRebalanceDetailSell.setFundName("中海沪港深价值优选灵活配置混合型证券投资基金");
        yfRebalanceDetailSell.setInvestmentAmount(new BigDecimal("48290.4087").setScale(2,BigDecimal.ROUND_HALF_DOWN));
        yfRebalanceDetailSell.setOrderDate("1524474269000");
        yfRebalanceDetailSell.setRedemptionUnit(new BigDecimal("28625.02").setScale(2,BigDecimal.ROUND_HALF_DOWN));
        yfRebalanceDetailSell.setSettlementDate("1525071599000");
        yfRebalanceDetailSell.setTransactionSettleLagDay("4");
        yfRebalanceDetailSell.setTransactionDate("1524553199000");
        yfRebalanceDetailSell.setTransactionCfmLagDay("1");
        yfRebalanceDetailSell.setTransactionStatus("received");
        yfRebalanceDetailSell.setTransactionType("sell");

        rebalanceResult.add(yfRebalanceDetailSell);

        YfRebalanceDetail yfRebalanceDetailBuy = new YfRebalanceDetail();
        yfRebalanceDetailBuy.setContractNumber("ORD20180423000059480");
        yfRebalanceDetailBuy.setFundCode("210012");
        yfRebalanceDetailBuy.setFundName("金鹰货币市场证券投资基金A类");
        yfRebalanceDetailBuy.setInvestmentAmount(new BigDecimal(28974.25).setScale(2,BigDecimal.ROUND_HALF_DOWN));
        yfRebalanceDetailBuy.setOrderDate("1524474270000");
        yfRebalanceDetailBuy.setRedemptionUnit(new BigDecimal(0).setScale(2,BigDecimal.ROUND_HALF_DOWN));
        yfRebalanceDetailBuy.setSettlementDate("1525104000000");
        yfRebalanceDetailBuy.setTransactionSettleLagDay("1");
        yfRebalanceDetailBuy.setTransactionDate("1524758400000");
        yfRebalanceDetailBuy.setTransactionCfmLagDay("1");
        yfRebalanceDetailBuy.setTransactionStatus("pending.payment");
        yfRebalanceDetailBuy.setTransactionType("buy");

        rebalanceResult.add(yfRebalanceDetailBuy);

        yfRebalancePortfolioResponse.setRebalanceResult(rebalanceResult);

        rebalanceService.rebalanceAfterDeal(yfRebalancePortfolioResponse,orderRebalanceSendBO);
    }


    @Test
    public void createRebalance(){
        try {
            fundRebalanceBiz.createRebalance();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }

    @Test
    public void sendRebalance(){
        try {
            fundRebalanceBiz.sendRebalance();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }





    @Test
    public void syncRebalance() throws Exception {
        fundRebalanceBiz.rebalanceOrderSync();
    }



}
