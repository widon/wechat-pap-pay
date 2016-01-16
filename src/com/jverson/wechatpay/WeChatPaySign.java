package com.jverson.wechatpay;

import java.util.Date;

/**
 * (类说明)<br/>
 * 微信免密支付用户签约类
 * @version  v1.0
 * 2015年11月26日 下午9:27:06 创建
 */
public class WeChatPaySign {

	/**
	 * id, 签约表主键
	 */
	private Long id; 
	/**
	 * 用户pin
	 */
	private String userPin; 
	/**
	 * 用户昵称
	 */
	private String nickName; 
	/**
	 * 委托代扣协议id（微信侧）
	 */
	private String contractId;
	/**
	 * 版本号，目前是1.0
	 */
	private String version;
	/**
	 * 模板id，查询签约需要
	 */
	private String planId; 
	/**
	 * 商户请求签约时传入的签约协议号，商户侧须唯一。
	 */
	private String contractCode; 
	/**
	 * 签约状态
	 * 1-已签约 2-初始化 3-已解约
	 */
	private Integer status; 
	/**
	 * 点分IP格式(客户端IP)
	 */
	private String clientIp ; 
	/**
	 * appid下用户唯一标示，签约后回调中返回，本地存储
	 */
	private String openId; 
	/**
	 * 协议到期时间
	 */
	private Date contractExpiredTime;
	/**
	 * 操作时间
	 */
	private Date operateTime;
	/**
	 * 签约时间
	 */
	private Date contractSignedTime;
	/**
	 * 签约请求序列号
	 */
	private String requestSerial;

	private String yn;
	
	
	
	
	/**
	 * @return the contractSignedTime
	 */
	public Date getContractSignedTime() {
		return contractSignedTime;
	}
	/**
	 * @param contractSignedTime the contractSignedTime to set
	 */
	public void setContractSignedTime(Date contractSignedTime) {
		this.contractSignedTime = contractSignedTime;
	}
	public String getYn() {
		return yn;
	}
	public void setYn(String yn) {
		this.yn = yn;
	}
	/**
	 * @return the contractExpiredTime
	 */
	public Date getContractExpiredTime() {
		return contractExpiredTime;
	}
	/**
	 * @param contractExpiredTime the contractExpiredTime to set
	 */
	public void setContractExpiredTime(Date contractExpiredTime) {
		this.contractExpiredTime = contractExpiredTime;
	}
	/**
	 * @return the operateTime
	 */
	public Date getOperateTime() {
		return operateTime;
	}
	/**
	 * @param operateTime the operateTime to set
	 */
	public void setOperateTime(Date operateTime) {
		this.operateTime = operateTime;
	}
	/**
	 * @return the nickName
	 */
	public String getNickName() {
		return nickName;
	}
	/**
	 * @param nickName the nickName to set
	 */
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	/**
	 * @return the clientIp
	 */
	public String getClientIp() {
		return clientIp;
	}
	/**
	 * @param clientIp the clientIp to set
	 */
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	/**
	 * @return the status
	 */
	public Integer getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(Integer status) {
		this.status = status;
	}
	/**
	 * @return the requestSerial
	 */
	public String getRequestSerial() {
		return requestSerial;
	}
	/**
	 * @param requestSerial the requestSerial to set
	 */
	public void setRequestSerial(String requestSerial) {
		this.requestSerial = requestSerial;
	}
	/**
	 * @return the openId
	 */
	public String getOpenId() {
		return openId;
	}
	/**
	 * @param openId the openId to set
	 */
	public void setOpenId(String openId) {
		this.openId = openId;
	}
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the userPin
	 */
	public String getUserPin() {
		return userPin;
	}
	/**
	 * @param userPin the userPin to set
	 */
	public void setUserPin(String userPin) {
		this.userPin = userPin;
	}
	/**
	 * @return the contractId
	 */
	public String getContractId() {
		return contractId;
	}
	/**
	 * @param contractId the contractId to set
	 */
	public void setContractId(String contractId) {
		this.contractId = contractId;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * @return the planId
	 */
	public String getPlanId() {
		return planId;
	}
	/**
	 * @param planId the planId to set
	 */
	public void setPlanId(String planId) {
		this.planId = planId;
	}
	/**
	 * @return the contractCode
	 */
	public String getContractCode() {
		return contractCode;
	}
	/**
	 * @param contractCode the contractCode to set
	 */
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}
	

	
	
}
