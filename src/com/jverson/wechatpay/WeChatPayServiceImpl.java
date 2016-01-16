package com.jverson.wechatpay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信免密支付Service
 * @version  v1.0
 */

@Service("weChatPayService")
public class WeChatPayServiceImpl implements WeChatPayService{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 组装签约请求的url
	 */
	@Override
	public String getSignRequestUrl(SortedMap<Object, Object> params, String userPin) {
		String signApiUrl = "";
		WeChatPaySign weChatPaySign = new WeChatPaySign();
		weChatPaySign.setUserPin(userPin);
		String nickName = "";
		//根据接口获取用户昵称，1：存入数据库 2：赋给签约入参contract_display_account
		if(StringUtils.isNotBlank(userPin)){
			weChatPaySign.setUserPin(userPin);
			try {
				UserBaseInfoVo userBaseInfoVo = userInfoExportService.getUserBaseInfoByPin(userPin, 3);
    	        if (userBaseInfoVo != null) {
    	        	nickName = userBaseInfoVo.getNickname();
    	        }else {
    	        	nickName = userPin;
    	        }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		weChatPaySign.setNickName(nickName);
		/**
		 * 初始状态设为-1，标识该用户只发送了签约请求，还未收到签约成功回调或者收到了未能成功更新状态
		 */
		weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_INIT);  
		/**
		 * 商户侧的签约协议号由uuid生成 32位
		 */
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		weChatPaySign.setContractCode(uuid);
		/**
		 * 请求序列号：随机的小于42位的无符号数字
		 */
		weChatPaySign.setRequestSerial(getSerial((String)params.get("clientip")));
		weChatPaySign.setClientIp((String)params.get("clientip"));
		weChatPaySign.setVersion(WeChatConstants.VERSION);
		weChatPaySign.setPlanId(WeChatConstants.PLAN_ID);
		/**
		 * 请求签约之前先检查是否存在该用户记录，无记录则将用户数据添加到签约表中，签约成功更新其签约状态字段即可
		 */
		WeChatPaySign checkExists = weChatPaySignDao.findByUserPin(userPin); 
		if(null == checkExists){
			Integer cnt = weChatPaySignDao.insertSignData(weChatPaySign); 
			if(cnt>0){
				logger.error("成功插入 "+userPin+" 的预签约记录~");
			}else{
				logger.error(userPin+" 的预签约记录写入异常！");
			}
		}else{
			weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_NO);  
			Integer cnt = weChatPaySignDao.updateByUserPin(weChatPaySign); //主要更新本地的RequestSerial和ContractCode，并将之前的ContractId重置为空
			if(cnt>0){
				logger.error("成功更新 "+userPin+" 的预签约记录~");
			}else{
				logger.error(userPin+" 的预签约记录更新异常！");
			}
		}
		/**
		 * 拼装签约请求必填参数
		 */
		params.remove("clientip");//请求签约的url去掉ip参数，否则会导致弹出浏览器选择框，本地依然会存储
		params.put("mch_id", WeChatConstants.MCH_ID); 
		params.put("appid", WeChatConstants.APP_ID); 
		params.put("plan_id", WeChatConstants.PLAN_ID); 
		params.put("contract_code", uuid); 
		params.put("request_serial", weChatPaySign.getRequestSerial()); 
		params.put("contract_display_account", nickName); 
		params.put("notify_url", WeChatConstants.SIGN_NOTIFY_URL); 
		params.put("version", WeChatConstants.VERSION); 
		params.put("timestamp", new Date().getTime()/1000); //时间戳限制10位
		params.put("return_app", 1); //时间戳限制10位
		String sign = WeChatPayUtil.getSignature(params);
		/**
		 * notify_url、contract_display_account(含有汉子) 需要在生成签名后encode
		 */
		String encodeUrl = "";
		String encodeNickName = "";
		try {
			encodeUrl = URLEncoder.encode(WeChatConstants.SIGN_NOTIFY_URL, "UTF-8");
			encodeNickName = URLEncoder.encode(nickName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("notify_url或nickName编码异常："+e);
			e.printStackTrace();
		}
		params.put("notify_url", encodeUrl); 
		params.put("contract_display_account", encodeNickName);
		signApiUrl = WeChatConstants.SIGN_CONTRACT_HTTP+"?"+WeChatPayUtil.getUrlParamsByMap((Map)params)+"&sign="+sign;
		logger.error(userPin + "得到的签约接口url：" + signApiUrl);
		
		return signApiUrl;
	}

	
	
	/* (non-Javadoc)
	 * 查询签约状态,先查本地状态再去微信端状态，以微信端状态为准，如果本地与微信端状态不一致则更新本地状态
	 */
	@Override
	public boolean checkSignStatus(String userPin) {
		SortedMap<Object, Object> params = new TreeMap<Object, Object>();
		params.put("mch_id", WeChatConstants.MCH_ID); //商户号
		params.put("appid", WeChatConstants.APP_ID); //公众账号id
		WeChatPaySign weChatPaySign = weChatPaySignDao.findByUserPin(userPin);
		if(null != weChatPaySign){
			if(null != weChatPaySign.getContractId()){
				params.put("contract_id", weChatPaySign.getContractId()); //使用contract_id模式查询
			}else if(null != weChatPaySign.getContractCode()){
				params.put("plan_id", WeChatConstants.PLAN_ID);
				params.put("contract_code", weChatPaySign.getContractCode()); //使用plan_id+contract_code模式
			}else{
				logger.error("contract_id 和 contract_code都为空");
				return false;
			}
			if(null != weChatPaySign.getVersion()){
				params.put("version", weChatPaySign.getVersion()); 
			}else{
				params.put("version", WeChatConstants.VERSION); //数据库无则用默认值1.0
			}
		}else{
			return false; //数据库没有记录则该用户第一次使用来点免密支付
		}
		
		Map<Object, Object> resultMap = WeChatPayUtil.HttpPostMethod(params, WeChatConstants.CHECK_SIGN_STATUS_HTTP, WeChatConstants.SOCKET_TIMEOUT, WeChatConstants.CONNECT_TIMEOUT);
		
		if("SUCCESS".equals(resultMap.get("valid"))){
				/**
				 * 已签约状态,此时如果本地不是已签约，则更新本地状态
				 */
				if(Integer.parseInt(resultMap.get("contract_state").toString())==0){
					logger.info("用户"+ resultMap.get("contract_display_account") +"为签约状态！");
					if(weChatPaySign.getStatus() != WeChatConstants.SIGN_STATUS_YES){
						weChatPaySign.setContractCode((String)resultMap.get("contract_code"));
						weChatPaySign.setContractId((String)resultMap.get("contract_id"));
						weChatPaySign.setPlanId((String)resultMap.get("plan_id"));
						weChatPaySign.setPlanId((String)resultMap.get("plan_id"));
						weChatPaySign.setOpenId((String)resultMap.get("openid"));
						weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_YES);
						weChatPaySign.setContractExpiredTime(WeChatPayUtil.strToDate((String)resultMap.get("contract_expired_time")));
						weChatPaySign.setContractSignedTime(WeChatPayUtil.strToDate((String)resultMap.get("contract_signed_time")));
						if(weChatPaySignDao.updateByContractCode(weChatPaySign)==1){
							logger.info("本地签约状态与微信端不一致，更新本地为已签约状态！");
						}
					}
					return true;
				}else if(Integer.parseInt(resultMap.get("contract_state").toString())==1){
					/**
					 * 未签约状态，如果本地为已签约状态，则更新本地状态
					 */
					if(weChatPaySign.getStatus() == WeChatConstants.SIGN_STATUS_YES){
						weChatPaySign.setContractCode((String)resultMap.get("contract_code"));
						weChatPaySign.setContractId((String)resultMap.get("contract_id"));
						weChatPaySign.setPlanId((String)resultMap.get("plan_id"));
						weChatPaySign.setOpenId((String)resultMap.get("openid"));
						weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_NO);
						weChatPaySign.setContractExpiredTime(WeChatPayUtil.strToDate((String)resultMap.get("contract_expired_time")));
						weChatPaySign.setContractSignedTime(WeChatPayUtil.strToDate((String)resultMap.get("contract_signed_time")));
						if(weChatPaySignDao.updateByContractCode(weChatPaySign)==1){
							logger.info("本地签约状态与微信端不一致，更新本地为已解约状态！");
						}
					}
					logger.info("用户"+ resultMap.get("contract_display_account") +"为解约状态！");
				}
		}else{
			logger.error("查询签约返回值校验签名失败！");
		}
		return false;
	}

	/* 
	 * 解约请求，true为解约成功，false为解约失败
	 */
	@Override
	public boolean terminateContract(String userPin) {
		SortedMap<Object, Object> params = new TreeMap<Object, Object>();
		params.put("mch_id", WeChatConstants.MCH_ID); 
		params.put("appid", WeChatConstants.APP_ID); 
		WeChatPaySign weChatPaySign = weChatPaySignDao.findByUserPin(userPin); 
		if(null != weChatPaySign){
			if(null != weChatPaySign.getContractId()){
				params.put("contract_id", weChatPaySign.getContractId()); //使用contract_id模式解约
			}else if(null!=weChatPaySign.getContractCode()){
				params.put("plan_id", WeChatConstants.PLAN_ID);
				params.put("contract_code", weChatPaySign.getContractCode()); //使用plan_id+contract_code模式
			}else{
				logger.error("contract_id为空或者plan_id和contract_code之一为空");
				return false;
			}

			if(null != weChatPaySign.getVersion()){
				params.put("version", weChatPaySign.getVersion()); 
			}else{
				params.put("version", WeChatConstants.VERSION); //数据库无则用默认值1.0
			}
		}else{
			logger.error("该用户没有签约记录！");
			return false; 
		}
		params.put("contract_termination_remark", "terminate from laidian"); //解约备注（待定）
        Map<Object, Object> resultMap = WeChatPayUtil.HttpPostMethod(params, WeChatConstants.TERMINATE_SIGN_HTTP, WeChatConstants.SOCKET_TIMEOUT, WeChatConstants.CONNECT_TIMEOUT);
		if("SUCCESS".equals(resultMap.get("valid"))){
			WeChatPaySign updateSignState = new WeChatPaySign();
			updateSignState.setContractCode(weChatPaySign.getContractCode());
			updateSignState.setContractId((String)resultMap.get("contract_id"));
			updateSignState.setStatus(WeChatConstants.SIGN_STATUS_NO);
			Integer cnt = weChatPaySignDao.updateByContractCode(updateSignState);
			if(cnt==1){
				QuickBind quickBind = new QuickBind();
				quickBind.setUserPin(userPin);
				quickBind.setPayWay(PayMent.wx_free_pay.getCode());
				quickBindDao.updatePayWay(quickBind);
				
				logger.info("解约成功！");
			}else{
				logger.error("解约成功，本地数据更新异常！ 更新记录： "+ cnt);
			}
			return true;
		}else{
			logger.error("解约返回值校验签名失败！");
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * 支付回调回调处理
	 */
	@Override
	public String handlePayNotify(String reqStr) {
		String confirm = null;
		Map<Object, Object> notifyMap = WeChatPayUtil.xmlParse(reqStr);
		if("SUCCESS".equals(notifyMap.get("return_code"))){
			if("SUCCESS".equals(notifyMap.get("result_code"))){
				/**
				 * 支付成功，这里将本地订单的支付状态更新为支付成功 (预订单处理)
				 */
				confirm = WeChatPayUtil.getConfirmXML("SUCCESS", "");
			}else{
				/**
				 * 支付失败，这里更新本地订单支付状态为支付失败，同时取消订单 (预订单处理)
				 */
				logger.error("错误代码:"+notifyMap.get("err_code")+" 错误原因描述："+notifyMap.get("err_code_des"));
			}
		}
		return confirm;
	}

	/* (non-Javadoc)
	 * 签约回调处理
	 */
	@Override
	public String handleSignNotify(String reqStr) {
		// TODO Auto-generated method stub
		String confirm = null;
		Map<Object, Object> notifyMap = WeChatPayUtil.xmlParse(reqStr);
		if(WeChatPayUtil.validateSign(notifyMap)){
			if("SUCCESS".equals(notifyMap.get("return_code"))){
				if("SUCCESS".equals(notifyMap.get("result_code"))){
					WeChatPaySign check = weChatPaySignDao.findByContractCode((String)notifyMap.get("contract_code")); //根据商户侧contract_code查找本地记录
					if(null != check){
						if((WeChatConstants.SIGN_STATUS_YES == check.getStatus())){  //如果本地已经是签约状态，说明回调已处理，直接回复即可
							confirm = WeChatPayUtil.getConfirmXML("SUCCESS", "");
							logger.error("handleSignNotify 本地签约状态无误，直接返回成功");
						}else if(WeChatConstants.SIGN_STATUS_YES != check.getStatus() && "ADD".equalsIgnoreCase((String)notifyMap.get("change_type"))){  
							WeChatPaySign weChatPaySign = new WeChatPaySign();
							weChatPaySign.setContractCode((String)notifyMap.get("contract_code"));
							weChatPaySign.setRequestSerial((String)notifyMap.get("request_serial"));
							weChatPaySign.setOpenId((String)notifyMap.get("openid"));
							weChatPaySign.setContractId((String)notifyMap.get("contract_id"));
							weChatPaySign.setOperateTime(WeChatPayUtil.strToDate((String)notifyMap.get("operate_time")));
							weChatPaySign.setContractExpiredTime(WeChatPayUtil.strToDate((String)notifyMap.get("contract_expired_time")));
							weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_YES);
							Integer cnt = weChatPaySignDao.updateByContractCode(weChatPaySign); //根据contract_code更新本地签约记录
							if(cnt==1){
								confirm = WeChatPayUtil.getConfirmXML("SUCCESS", "");
								logger.error("handleSignNotify 本地签约状态已根据notify更新为最新状态");
							}else{
								logger.error("更新本地签约数据发生异常： "+cnt);
							}
						}
					}
				}else{
					logger.error("错误代码:"+notifyMap.get("err_code")+" 错误原因描述："+notifyMap.get("err_code_des"));
				}
			}
		}else{
			logger.error("handleSignNotify 校验签名失败！");
		}
		
		return confirm;
	}


	/* (non-Javadoc)
	 * 微信端发生解约时调用
	 * 解约回调处理服务
	 */
	@Override
	public String handleTerminateNotify(String reqStr) {
		// TODO Auto-generated method stub
		String confirm = null;
		Map<Object, Object> notifyMap = WeChatPayUtil.xmlParse(reqStr);
		if(WeChatPayUtil.validateSign(notifyMap)){
			if("SUCCESS".equals(notifyMap.get("return_code"))){
				if("SUCCESS".equals(notifyMap.get("result_code"))){
					WeChatPaySign check = weChatPaySignDao.findByContractCode((String)notifyMap.get("contract_code")); //根据商户侧contract_code查找本地记录
					if(null != check){
						if((WeChatConstants.SIGN_STATUS_NO == check.getStatus())){  //如果本地已经是解约状态，直接回复即可
							confirm = WeChatPayUtil.getConfirmXML("SUCCESS", "");
						}else if(WeChatConstants.SIGN_STATUS_NO != check.getStatus() && "DELETE".equalsIgnoreCase((String)notifyMap.get("change_type"))){  
							WeChatPaySign weChatPaySign = new WeChatPaySign();
							weChatPaySign.setContractCode((String)notifyMap.get("contract_code"));
							weChatPaySign.setRequestSerial((String)notifyMap.get("request_serial"));
							weChatPaySign.setOpenId((String)notifyMap.get("openid"));
							weChatPaySign.setContractId((String)notifyMap.get("contract_id"));
							weChatPaySign.setOperateTime(WeChatPayUtil.strToDate((String)notifyMap.get("operate_time")));
							weChatPaySign.setContractExpiredTime(WeChatPayUtil.strToDate((String)notifyMap.get("contract_expired_time")));
							weChatPaySign.setStatus(WeChatConstants.SIGN_STATUS_NO);
							Integer cnt = weChatPaySignDao.updateByContractCode(weChatPaySign); //根据contract_code更新本地签约记录
							
							// 删除成功，所有的使用微信免密支付的来点都修改为货到付款
							WeChatPaySign sign = weChatPaySignDao.findByContractCode((String)notifyMap.get("contract_code"));
							QuickBind quickBind = new QuickBind();
							quickBind.setUserPin(sign.getUserPin());
							quickBind.setPayWay(PayMent.wx_free_pay.getCode());
							quickBindDao.updatePayWay(quickBind);
							
							if(cnt==1){
								confirm = WeChatPayUtil.getConfirmXML("SUCCESS", "");
							}else{
								logger.error("handleTerminateNotify 更新本地签约数据发生异常： "+cnt);
							}
						}
					}
				}else{
					logger.error("错误代码:"+notifyMap.get("err_code")+" 错误原因描述："+notifyMap.get("err_code_des"));
				}
			}
		}else{
			logger.error("handleTerminateNotify 校验签名失败！");
		}


		return confirm;
	}

	/**
	 * 获取随机的小于42位的无符号数字
	 * @param ipAddress ip地址
	 * @return 随机数字<br/>
	 */
	private String getSerial(String ipAddress) {
        long result = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << (i * 8);
        }
        StringBuffer seriBuffer = new StringBuffer(String.valueOf(result));
        seriBuffer.append(String.valueOf(System.currentTimeMillis()));
        seriBuffer.append(String.valueOf(RandomUtils.nextLong()));
        return  seriBuffer.toString();
    }
	
}

