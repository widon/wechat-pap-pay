/**
 * WeChatPayService.java
 * 功  能： （用一句话描述类的功能）
 * 类名：  WeChatPayService
 * Copyright (c) 2015 jverson.com All rights reserved.
 * 本软件源代码版权归jverson.com所有,未经许可不得任意复制与传播.
 */
package com.jverson.wechatpay;

import java.util.SortedMap;



/**
 * (类说明)<br/>
 * 微信免密支付Service
 * @version  v1.0
 * 2015年11月26日 上午11:41:48 创建
 */
public interface WeChatPayService {
	
	/**
	 * 组装并返回签约url给app，跳转到微信签约页面
	 * @return Map<Object,Object>
	 */
	public String getSignRequestUrl(SortedMap<Object, Object> params, String userPin);
	
	/**
	 *  查询签约接口，已签约返回true，未签约返回false
	 * @param xmlParams
	 * @return boolean
	 */
	public boolean checkSignStatus(String userPin);
	
	/**
	 *  解除签约服务，解除成功返回true，失败返回false
	 * @param params
	 * @return boolean
	 */
	public boolean terminateContract(String userPin);
	
	/**
	 * 微信免密支付接口
	 * @param params
	 * @return Map
	 */
//	public Map<Object, Object> autoWeChatPay(QuickOrderWrap quickOrderWrap);
	
	/**
	 * 订单查询服务
	 * @param transaction_id微信订单号或out_trade_no商户订单号
	 * @return boolean
	 */
//	public Map<Object, Object> checkWeChatPay(String transaction_id, String out_trade_no);
	
	/**
	 * 签约回调处理服务
	 * @param reqStr
	 * @return Object
	 */
	public String handleSignNotify(String reqStr);

	/**
	 * 支付回调处理服务
	 * @param reqStr
	 * @return Object
	 */
	public String handlePayNotify(String reqStr);
	
	/**
	 * 退款服务
	 * @param params
	 * @return Map<Object,Object>
	 */
//	public Map<Object, Object> refund(Map<Object, Object> params);

	/**
	 * 解约回调处理
	 * @param reqStr
	 * @return String
	 */
	public String handleTerminateNotify(String reqStr);
	
}
