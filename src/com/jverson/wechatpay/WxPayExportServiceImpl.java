package com.jverson.wechatpay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 微信支付接口<br/>
 */
@Controller
public class WxPayExportServiceImpl implements WxPayExportService {

	private  static Logger logger = LoggerFactory.getLogger(Logger.class);
	
	@Profiled(tag = "payRequestWithoutPwd")
    @Override
    public WxPayResultVo payRequestWithoutPwd(WxPayVo wxPayVo) {
    	
		WxPayResultVo wxPayResultVo = new WxPayResultVo();
    	String agencyCode = wxPayVo.getAgencyCode();
    	/**------------------------------
    	 * 
    	 * 获取商户信息
    	 * 
    	 * ------------------------------
    	 */
        Properties mchInfoProp = PropertiesInfoHelper.readPropertiesFile("/props/wx_info_" + agencyCode + ".properties");
        String appId = mchInfoProp.getProperty("appid");
        String mchId = mchInfoProp.getProperty("mch_id");
        String wxKey = mchInfoProp.getProperty("key");
        String nonceStr = WxPayUtil.createRandomString(30);
        
        /**------------------------------
         * 
         * 生成签名，拼装请求参数
         * 
         * ------------------------------
         */
    	SortedMap<Object, Object> sortedMap = new TreeMap<Object, Object>();
    	//----------商户参数------------
    	sortedMap.put("appid", appId);
    	sortedMap.put("mch_id", mchId);
    	sortedMap.put("nonce_str", nonceStr);
    	//----------支付参数------------
    	if(null != wxPayVo.getBody()){
    		sortedMap.put("body", wxPayVo.getBody());
    	}else{
    		sortedMap.put("body", "test-order");
    	}
    	sortedMap.put("out_trade_no", wxPayVo.getOutTradeNo()); //支付单号
    	sortedMap.put("total_fee", wxPayVo.getTotal_fee()); //单位为分
    	sortedMap.put("spbill_create_ip", ConfigConstants.TXPAY_SERVER_IP); //调用支付机器ip
    	sortedMap.put("notify_url", ConfigConstants.TXPAY_NOTIFY_URL); //支付回调url设置
    	sortedMap.put("trade_type", ConfigConstants.TXPAY_TRADE_TYPE); //微信免密为“PAP”
    	sortedMap.put("contract_id", wxPayVo.getContract_id());
    	if(null != wxPayVo.getAttach()){
    		sortedMap.put("attach", wxPayVo.getAttach());
    	}
    	//----------生成签名------------
        String sign = WxPayUtil.getSignature(sortedMap, wxKey); // 生成签名
        sortedMap.put("sign", sign);
    	logger.info("扣款请求参数为："+GsonUtils.toJson(sortedMap));
        /**------------------------------
         * 
         * 请求微信扣款接口，返回结果处理
         * 
         * ------------------------------
         */
        try {
        	//----------http post 请求微信扣款接口------------
        	Map<Object, Object> resultMap = WxPayUtil.HttpPostMethod(sortedMap, ConfigConstants.PAY_CHARGE_HTTP, 
    				ConfigConstants.SOCKET_TIMEOUT, ConfigConstants.CONNECT_TIMEOUT, wxKey);
        	wxPayResultVo.setIsSuccess(false); //结果初始化为false
    		logger.info("OutTradeNo:"+wxPayVo.getOutTradeNo()+" payRequestWithoutPwd 返回结果："+GsonUtils.toJson(resultMap));
    		//----------微信扣款接口返回结果处理-----------------
    		if ("SUCCESS".equals(resultMap.get("return_code"))) {
                if ("SUCCESS".equals(resultMap.get("result_code"))) {
                	if("SUCCESS".equals(resultMap.get("valid"))){
            			wxPayResultVo.setIsSuccess(true);
            			logger.info("微信免密支付扣款请求校验成功并完成支付，OutTradeNo:"+wxPayVo.getOutTradeNo());
            			return wxPayResultVo;
            		}else{
            			wxPayResultVo.setCode(ErrorInfo.ERROR_80000.getCode());
            			wxPayResultVo.setInfo(ErrorInfo.ERROR_80000.getInfo());
            			logger.error("查询扣款返回值校验签名失败！");
            		}
                }else{
                	logger.error("OutTradeNo:"+wxPayVo.getOutTradeNo()+"错误代码:"+resultMap.get("err_code")+" 错误原因描述："+resultMap.get("err_code_des"));
                	wxPayResultVo.setCode(resultMap.get("err_code").toString());
        			wxPayResultVo.setInfo(resultMap.get("err_code_des").toString());
        			//----------系统超时或银行端超时，需要立即调用被扫订单结果查询API，查询当前订单状态，并根据订单的状态决定下一步的操作-----------
        			if("SYSTEMERROR".equals(resultMap.get("err_code")) || "BANKERROR".equals(resultMap.get("err_code"))){
        				WxCheckPayResultVo wxCheckPayResultVo = this.checkWeChatPay(wxPayVo);
        				if(null != wxCheckPayResultVo && wxCheckPayResultVo.getIsSuccess()){
        					wxPayResultVo.setIsSuccess(true);
        				}else{
        					BeanUtils.copyProperties(wxPayResultVo, wxCheckPayResultVo);
        				}
        			}
                }
            }else{
            	wxPayResultVo.setCode(ErrorInfo.ERROR_70000.getCode());
    			wxPayResultVo.setInfo(resultMap.get("return_msg").toString());
            }
    		
		} catch (Exception e) {
			logger.error("payRequestWithoutPwd: ", e);
		}
		
		return wxPayResultVo;
    }

	/* (non-Javadoc)
	 * 订单查询服务
	 * @param params 入参：
     * WxPayVo：微信端订单号transaction_id，商户端订单号out_trade_no，两者传其一即可
     * @return 接口调用结果：支付结果
	 */
	@Profiled(tag = "checkWeChatPay")
	@Override
	public WxCheckPayResultVo checkWeChatPay(WxPayVo wxPayVo) {
		WxCheckPayResultVo wxCheckPayResultVo = new WxCheckPayResultVo();
		/**----------------------------------
		 * 
		 * 获取商户信息
		 * 
		 * ----------------------------------
		 */
    	Properties mchInfoProp = PropertiesInfoHelper.readPropertiesFile("/props/wx_info_" + wxPayVo.getAgencyCode() + ".properties");
        String appId = mchInfoProp.getProperty("appid");
        String mchId = mchInfoProp.getProperty("mch_id");
        String wxKey = mchInfoProp.getProperty("key");
        String nonceStr = WxPayUtil.createRandomString(30);
        /**----------------------------------
         * 
         * 签名拼装传参数
         * 
         * ----------------------------------
         */
		SortedMap<Object, Object> sortedMap = new TreeMap<Object, Object>();
        sortedMap.put("mch_id", mchId); //商户号
        sortedMap.put("appid", appId); //公众账号id
        sortedMap.put("nonce_str", nonceStr); //随机字符串
        sortedMap.put("nonce_str", WxPayUtil.createRandomString(32)); //随机字符串
		if(null != wxPayVo.getTransactionId()){
			sortedMap.put("transaction_id", wxPayVo.getTransactionId());
		}else if(null != wxPayVo.getOutTradeNo()){
			sortedMap.put("out_trade_no", wxPayVo.getOutTradeNo());
		}else{
			logger.error("checkWeChatPay请传入订单号查询!"+wxPayVo.getOutTradeNo());
		}
		String sign = WxPayUtil.getSignature(sortedMap, wxKey); // 生成签名
        sortedMap.put("sign", sign);
        /**----------------------------------
         * 
         * 调用支付查询接口，处理查询结果
         * 
         * ----------------------------------
         */
		try {
        	//----------http post 请求微信查询支付接口------------
        	Map<Object, Object> resultMap = WxPayUtil.HttpPostMethod(sortedMap, ConfigConstants.CHECK_CHARGE_HTTP, 
    				ConfigConstants.SOCKET_TIMEOUT, ConfigConstants.CONNECT_TIMEOUT, wxKey);
        	wxCheckPayResultVo.setIsSuccess(false); //结果初始化为false
    		logger.info("OutTradeNo:"+wxPayVo.getOutTradeNo()+" checkWeChatPay 返回结果："+GsonUtils.toJson(resultMap));
    		//----------微信查询支付接口返回结果处理-----------------	
			if ("SUCCESS".equals(resultMap.get("return_code"))) {
                if ("SUCCESS".equals(resultMap.get("result_code"))) {
                	if("SUCCESS".equals(resultMap.get("valid"))){
                		if("SUCCESS".equals(resultMap.get("trade_state"))){
                			wxCheckPayResultVo.setIsSuccess(true);
                			logger.info("该笔订单已支付，OutTradeNo:"+wxPayVo.getOutTradeNo());
        				}else if("REFUND".equals(resultMap.get("trade_state"))){
        					wxCheckPayResultVo.setCode(ErrorInfo.ERROR_80002.getCode());
        					wxCheckPayResultVo.setInfo(ErrorInfo.ERROR_80002.getInfo());
        					logger.info("该笔订单已转入退款！OutTradeNo:"+wxPayVo.getOutTradeNo());
        				}else if("ACCEPTED".equals(resultMap.get("trade_state"))){
        					wxCheckPayResultVo.setCode(ErrorInfo.ERROR_80003.getCode());
        					wxCheckPayResultVo.setInfo(ErrorInfo.ERROR_80003.getInfo());
        					logger.info("申请已受理！OutTradeNo:"+wxPayVo.getOutTradeNo());
        				}else if("PAYERROR".equals(resultMap.get("trade_state"))){
        					wxCheckPayResultVo.setCode(ErrorInfo.ERROR_80004.getCode());
        					wxCheckPayResultVo.setInfo(ErrorInfo.ERROR_80004.getInfo());
        					logger.error("支付失败(其他原因，如银行返回失败)！OutTradeNo:"+wxPayVo.getOutTradeNo());
        				}else{
        					wxCheckPayResultVo.setCode(ErrorInfo.ERROR_80008.getCode());
        					wxCheckPayResultVo.setInfo(ErrorInfo.ERROR_80008.getInfo());
        					logger.error("查询交易状态返回未知异常！OutTradeNo:"+wxPayVo.getOutTradeNo());
        				}
            		}else{
            			wxCheckPayResultVo.setCode(ErrorInfo.ERROR_80000.getCode());
            			wxCheckPayResultVo.setInfo(ErrorInfo.ERROR_80000.getInfo());
            			logger.error("查询扣款返回值校验签名失败！OutTradeNo:"+wxPayVo.getOutTradeNo());
            		}
                }else{
                	logger.error("OutTradeNo:"+wxPayVo.getOutTradeNo()+"错误代码:"+resultMap.get("err_code")+" 错误原因描述："+resultMap.get("err_code_des"));
                	wxCheckPayResultVo.setCode(resultMap.get("err_code").toString());
                	wxCheckPayResultVo.setInfo(resultMap.get("err_code_des").toString());
				}
            }else{
            	wxCheckPayResultVo.setCode(ErrorInfo.ERROR_70000.getCode());
            	wxCheckPayResultVo.setInfo(resultMap.get("return_msg").toString());
            }
		} catch (Exception e) {
			logger.error("checkWeChatPay: ", e);
		}
        
		return wxCheckPayResultVo;
	}

	
	@Override
	public String handlePayNotify(String reqStr) throws IOException {
		logger.info("handlePayNotify begin. received PayNotify: "+reqStr);
		String confirm = WxPayUtil.getConfirmXML("FAIL", "");
		try {
			if(null != reqStr && !"".equals(reqStr)){
				logger.info(reqStr);
				confirm = this.handleNotifyData(reqStr);
			}else{
				logger.error("获取微信签约notify为空！");
			}
		} catch (Exception e) {
			logger.error("签约结果回调发生异常：", e);
		} 
		return confirm;
	}
	
	/**
	 * 微信支付异步通知结果处理
	 * @param reqStr
	 * @return
	 */
	public String handleNotifyData(String reqStr) {
		logger.info("handleNotifyData begin..."+ reqStr);
		String confirm = null;
		Map<Object, Object> notifyMap = WxPayUtil.xmlParse(reqStr);
		logger.info("out_trade_no"+notifyMap.get("out_trade_no")+" 收到notify："+ GsonUtils.toJson(notifyMap));
		if("SUCCESS".equals(notifyMap.get("return_code"))){
			if("SUCCESS".equals(notifyMap.get("result_code"))){
				
		    	//----------使用attach回传字段恢复payOrderSaveReqVo对象用于创建任务单-----------------
				logger.info("创建任务单 begin..."+ notifyMap.get("out_trade_no"));
				PayOrderSaveReqVo payOrderSaveReqVo = new PayOrderSaveReqVo();
				String attachString = notifyMap.get("attach").toString();
				JSONObject  jObj = JSONObject.parseObject(attachString);
				PaymentMsgBean paymentMsgBean = new PaymentMsgBean();
				paymentMsgBean.setPin(jObj.getString("Pin"));
				paymentMsgBean.setAgencyCode(jObj.getString("agencyCode"));
				paymentMsgBean.setAmount(new BigDecimal(jObj.getString("amount")));
				logger.info("attach 转换为paymentMsgBean对象："+ GsonUtils.toJson(paymentMsgBean));
				
				//--------------paymentMsgBean对象回传了AgencyCode、Pin、amount微信回传PayId、BankOrderNo----------------- 
				paymentMsgBean.setPayId(notifyMap.get("out_trade_no").toString()); 
				paymentMsgBean.setBankOrderNo(notifyMap.get("transaction_id").toString());
		        QueryPayOrderReqVo queryPayOrderReqVo = createQueryPayOrderReqVo(paymentMsgBean);
		        logger.info("queryPayOrder begin...notify请求交易系统参数，request payTrade queryPayOrder parameters:" + GsonUtils.toJson(queryPayOrderReqVo));
		        QueryPayOrderResVo queryPayOrderResVo = payOrderResource.queryPayOrder(queryPayOrderReqVo);
		        logger.info("queryPayOrder end...notify交易系统返回参数：payTrade response queryPayOrder parameters:" + GsonUtils.toJson(queryPayOrderResVo));
		        payOrderSaveReqVo = this.getPayOrderSaveReqVo(queryPayOrderResVo);
		        
		        //--------------返回结果每次都需要校验签名----------------- 
		        logger.info("获取商户信息begin...parameters:" + GsonUtils.toJson(payOrderSaveReqVo));
				String agencyCode = payOrderSaveReqVo.getAgencyCode();
				Properties mchInfoProp = PropertiesInfoHelper.readPropertiesFile("/props/wx_info_" + agencyCode + ".properties");
		        String wxKey = mchInfoProp.getProperty("key");
                if (WxPayUtil.validateSign(notifyMap, wxKey)) {
                	/**
    				 * 支付成功，这里将本地订单的支付状态更新为支付成功 (预订单处理)
    				 */
                	logger.info("回调参数校验签名成功！");
    				/***------------------  生成任务单-------------------start--------------------------------------------------------------------------***/
    				InsertPayMainTaskReqVo insertPayMainTaskReqVo = WxInstanceUtils.createInsertPayMainTaskReqVo(payOrderSaveReqVo);
    		    	/*** 调用交易系统生成支付任务 */
    		    	try{
    		    		logger.info("调用交易系统生成支付任务 begin...parameters:" + GsonUtils.toJson(insertPayMainTaskReqVo));
//    		    		InsertPayMainTaskResVo resVo  = commonLogicService.retryCreatePayMainTask(WxCommonConstants.PAY_VIRTUAL_TIME_OUT_TRY_START_NUM,WxCommonConstants.PAY_VIRTUAL_TIME_OUT_TRY_END_NUM,insertPayMainTaskReqVo);
    		    		InsertPayMainTaskResVo resVo  = this.retryCreatePayMainTask(WxCommonConstants.PAY_VIRTUAL_TIME_OUT_TRY_START_NUM,WxCommonConstants.PAY_VIRTUAL_TIME_OUT_TRY_END_NUM,insertPayMainTaskReqVo);
    		    		confirm = WxPayUtil.getConfirmXML("SUCCESS", "");
    		    		logger.info("调用交易系统生成支付任务 end...result:"+GsonUtils.toJson(resVo));
    		    	}catch(Exception e){
    		    		logger.error("支付任务失败---"+WxCommonConstants.PAY_VIRTUAL_CREATE_MAINTASK_TIMEOUT_ERRORCODE_INFO+"_"+WxCommonConstants.PAY_VIRTUAL_CREATE_MAINTASK_TIMEOUT_ERRORCODE+"_"+GsonUtils.toJson(insertPayMainTaskReqVo),e);
    		    	}
    		    	/***------------------  生成任务单-------------------end--------------------------------------------------------------------------***/
                
                } else {
                    logger.error("out_trade_no"+notifyMap.get("out_trade_no")+" 收到notify校验签名失败");
                }
			}else{
				/**
				 * 支付失败，这里更新本地订单支付状态为支付失败，同时取消订单 (预订单处理)
				 */
				logger.error("错误代码:"+notifyMap.get("err_code")+" 错误原因描述："+notifyMap.get("err_code_des"));
			}
		}
		return confirm;
	}

}
