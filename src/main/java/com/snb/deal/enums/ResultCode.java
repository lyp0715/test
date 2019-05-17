package com.snb.deal.enums;

import com.snb.common.dto.IResultCode;

/**
 *  2 交易 3：赎回
 * @Description 
 * @author lizengqiang
 * @date 2018/4/17 10:48
 */
public enum ResultCode implements IResultCode {

    //21开头 - 计划
    PLAN_NO_RESPONSE(21002,"请求无响应"),
    PLAN_NO_PLAN(21005,"查询计划失败"),
    PLAN_ERROR_STATUS(21006,"计划状态异常"),
    PLAN_NO_PORTFOLIO_REL(21007,"查询计划持仓关系失败"),

    //创建计划
    PLAN_CREATE_NO_FIND_MAINMODEL(21001,"查询主理人模型失败"),
    PLAN_CREATE_FAILED(21003,"计划创建失败"),
    PLAN_CREATE_INIT_FAILED(21004,"计划信息初始化失败"),

    //修改计划
    PLAN_MODIFY_REQ_FAILED(21106,"修改定投计划请求失败"),
    PLAN_MODIFY_FAILED(21107,"修改定投计划失败"),

    //暂停计划
    PLAN_SUSPEND_REQ_FAILED(21201,"暂停计划请求失败"),
    PLAN_SUSPEND_FAILED(21202,"暂停计划失败"),

    //重启计划
    PLAN_RESTART_REQ_FAILED(21301,"重启计划请求失败"),
    PLAN_RESTART_FAILED(21302,"重启计划失败"),

    //计划组合
    PLAN_PORTFOLIO_REQ_FAILED(21401,"查询组合账户请求失败"),

    //22开头 - 投资
    INVEST_NO_PORTFOLIO(22001,"查询用户计划持仓失败"),
    INVEST_NO_PORTFOLIOID(22002,"用户计划无持仓"),
    INVEST_SAVE_ORDER_FAILED(22003,"保存订单失败"),
    INVEST_REQ_FAILED(22004,"组合投资发起失败"),
    AUTO_INVEST_NO_PLAN(22005,"定投查询计划信息失败"),
    AUTO_INVEST_QUERY_FAILED(22006,"第三方订单查询失败"),
    INVEST_NO_FUND_ACCOUNT(22007,"查询基金账户失败"),
    INVEST_FAIL(22008,"买入失败"),
    INVEST_NO_FUND_MAIN_MODEL_DETAIL(22009,"买入没有获取到主理人模型详情"),

    //23开头 - 赎回
    PORTFOLIO_ERROR(23012, "持仓模型数据异常"),
    NO_FIND_ACCOUNT_NUMBER(23013, "获取投资账户为空"),
    INVESTOR_PAY_ID_ERROR(23014, "支付代码异常"),
    ACCOUNT_NUMBER_ERROR(23015, "获取投资账户异常"),
    REDEEM_APPLY_ERROR(23016, "发起赎回失败"),
    REDEEM_CHECK_ERROR(23017, "获取赎回校验信息为空"),
    REDEEM_CHECK_SUCCESS(23020, "用户赎回校验成功"),
    PORTFOLIO_ACCOUNT_NULL(23021, "获取持仓账户数据为空"),
    PORTFOLIO_AVAILABLE_AMOUNT(23022, "组合可用金额小于0"),
    PORTFOLIO_MAX_REDEMPTION_AMOUNT(23023, "组合最高赎回金额小于0"),
    PORTFOLIO_MIN_REDEMPTION_AMOUNT(23024, "组合最低赎回金额小于0"),
    MIN_REDEMPTION_AMOUNT_ERROR(23025, "赎回金额小于组合最低赎回金额"),
    AVAILABLE_AMOUNT_ERROR(23026, "赎回金额大于组合可用金额"),
    MAX_REDEMPTION_AVAILABLE_AMOUNT_ERROR(23027, "赎回金额大于组合最高赎回金额且小于组合可用金额,应该全部赎回"),
    PORTFOLIO_ACCOUNT_ERROR(23028, "获取持仓账户数据异常"),
    REDEEM_FEE_ERROR(23029, "获取费率，预估费用失败"),

    // deal 计划相关
    PLAN_PORTFOLIO_ACCOUNT_ERROR(21011, "获取计划持仓账户数据异常"),
    PLAN_STATISTICS_ERROR(21012, "获取计划统计数据异常"),
    PLAN_INFO_ERROR(21013, "获取计划数据异常"),
    PLAN_PORTFOLIO_REL_LIST_ERROR(21014, "获取计划持仓关系列表数据异常"),
    BUY_TRANSACTION_AND_EXPECTED_CONFIRMED_DATE_ERROR(21015, "获取买入预计交易日期和预计交易确认日期异常"),
    USER_INCOME_INFO_ERROR(21016, "获取用户总资产、累计收益、昨日收益异常"),
    PLAN_ASSET_DETAIL_INFO_ERROR(21017, "获取计划可赎回、计划待确认资产详情异常"),
    PLAN_ASSET_INFO_ERROR(21018, "获取计划可赎回资产、累计收益、确认中资产异常"),
    PLAN_INFO_LIST_ERROR(21019, "获取计划列表异常"),

    // deal 计划-保险相关，从后3位从101开始
    PLAN_INSURANCE_ERROR(21101, "获取保险数据异常"),
    PLAN_GET_UPLOAD_INSURANCE_ERROR(21102, "保存上传的用户保险信息异常"),

    //订单相关
    ORDER_INVEST_FEE_ERROR(22002, "获取买入相关费用异常")



    ;

    int code;
    String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }
}
