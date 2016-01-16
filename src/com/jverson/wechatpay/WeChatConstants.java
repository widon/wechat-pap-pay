package com.jverson.wechatpay;

public class WeChatConstants {

	
	// =======【基本信息设置】=========================
	/**
	 * 公共账号id
	 */
	public static String APP_ID = "wxcbhhkjsdhf345sf3jfye"; 
	/**
	 * 商户号
	 */
	public static String MCH_ID = "123456789"; 
	/**
	 * 商户支付密钥，参考开户邮件设置
	 */
	public static String KEY = "jhsgfJ6sjgDKHDSudii79U01abc";
	/**
	 * 协议模板id
	 */
	public static String PLAN_ID = "10006";
	/**
	 * 版本号
	 */
	public static String VERSION = "1.0";
	/**
	 * 传输超时
	 */
	public static Integer SOCKET_TIMEOUT = 5000;
	/**
	 * 请求超时
	 */
	public static Integer CONNECT_TIMEOUT = 5000;
	
	// =======【签约接口及回调url】======================
	
	/**
	 * /签约结果通知回调url，用于商户接收签约结果
	 */
	public static String SIGN_NOTIFY_URL = "https://jverson.com/wechat/signNotify";
	/**
	 * /签约接口url
	 */
	public static final String SIGN_CONTRACT_HTTP = "https://api.mch.weixin.qq.com/papay/entrustweb";
	/**
	 * /查询签约状态接口url
	 */
	public static final String CHECK_SIGN_STATUS_HTTP = "https://api.mch.weixin.qq.com/papay/querycontract";
	/**
	 * /解约接口url
	 */
	public static final String TERMINATE_SIGN_HTTP = "https://api.mch.weixin.qq.com/papay/deletecontract";
	/**
	 * 解约通知url，本地提供给微信，后面可能需要改为https
	 */
	public static final String TERMINATE_NOTIFY_HTTP = "https://jverson.com/wechat/terminateNotify";
	
	// =======【签约状态】=============================
	/**
	 * 初始状态，在签约回调中将其更新之前为无效数据
	 */
	public static final Integer SIGN_STATUS_INIT = -1;
	/**
	 * 已签约状态
	 */
	public static final Integer SIGN_STATUS_YES = 1;
	/**
	 * 已解约状态
	 */
	public static final Integer SIGN_STATUS_NO = 2;
	
	
	// =======【支付接口及回调url】======================
	
	/**
	 * /支付结果通知回调url，用于商户接收支付结果
	 */
	public static String PAY_NOTIFY_URL = "now.jd.com";
	/**
	 * /扣款接口url
	 */
	public static final String PAY_CHARGE_HTTP = "https://api.mch.weixin.qq.com/pay/pappayapply";
	/**
	 * /订单查询接口url
	 */
	public static final String CHECK_CHARGE_HTTP = "https://api.mch.weixin.qq.com/pay/paporderquery";
	/**
	 * 退款接口url
	 */
	public static final String REFUND_CHARGE_HTTP = "https://api.mch.weixin.qq.com/secapi/pay/refund";
	/**
	 * 账户余额不足
	 */
	public static final String PAY_ERROR_LACK = "账户余额不足，建议更换银行卡支付";
	/**
	 * 该卡不支持当前支付，请换卡支付或绑新卡支付
	 */
	public static final String PAY_ERROR_UNSOPPORTED = "该卡不支持当前支付，请换卡支付或绑新卡支付";
	/**
	 * 账户余额不足
	 */
	public static final String PAY_ERROR_EXCEED = "账户支付已达上限，建议您更换银行卡支付";
	
	
	
	
}
