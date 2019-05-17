package com.snb.remote;

import com.snb.BaseBeanTest;
import com.snb.common.datetime.DateTimeUtil;
import com.snb.deal.api.dto.invest.InvestRequest;
import com.snb.deal.api.remote.order.OrderInvestRemote;
import com.snb.user.util.JSONUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * USER:    huangyunxing
 * TIME:    2018-07-13 11:23
 * COMMENT:
 */
public class TestOrderInvestRemote extends BaseBeanTest {
    @Resource
    private OrderInvestRemote remote;

    @Test
    public void testInvest() {
        String json = "{\"channel\":\"YIFENG\",\"fundUserAccount\":\"JLC20180709000028960\",\"fundUserAccountId\":67106447482944,\"investAmount\":10000,\"investorPayId\":\"11773\",\"planId\":67106644975616,\"userId\":\"ab5c08fb193348979675b8ef65657664\"}";
        InvestRequest request = JSONUtil.parse(json, InvestRequest.class);
        remote.singleInvest(request);
    }


    @Test
    public void testDate(){
        List<String> date = new ArrayList<String>();
        date.add("2018-07-30");
        date.add("2018-07-31");
        date.add("2018-08-01");
        date.add("2018-08-02");
        date.add("2018-08-03");
        date.add("2018-08-04");
        date.add("2018-08-05");
        for(int i=0;i<date.size();i++){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date expectedConfirmDate = null;
            try{
                expectedConfirmDate = sdf.parse(date.get(i));
            }catch (Exception e){
                e.printStackTrace();
            }

            expectedConfirmDate = caculateExpectedConfirmDate(expectedConfirmDate);

            LocalDate expectedIncomeDateLocalDate = DateTimeUtil.toLocalDate(expectedConfirmDate).plusDays(1);
            Date expectedIncomeDate = DateTimeUtil.toDate(expectedIncomeDateLocalDate);
            expectedIncomeDate = caculateExpectedConfirmDate(expectedIncomeDate);

            System.out.println(date.get(i)+"日预计确认日期expectedConfirmDate:"+expectedConfirmDate);
            System.out.println(date.get(i)+"日预计收益日期expectedIncomeDateLocalDate:" + expectedIncomeDate);
        }
    }

    public final static Date caculateExpectedConfirmDate(Date date) {

        if (null != date) {
            DateTime dateTime = new DateTime(date.getTime());
            if (DateTimeConstants.SATURDAY == dateTime.getDayOfWeek()) {
                return dateTime.plusDays(2).toDate();
            }
            if (DateTimeConstants.SUNDAY == dateTime.getDayOfWeek()) {
                return dateTime.plusDays(1).toDate();
            }

        }
        return date;
    }
}
